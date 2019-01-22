/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.batchupdater

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Millis, Span}
import org.scalatest.{Matchers, WordSpec}
import uk.gov.hmrc.batchupdater.SingleResult._
import uk.gov.hmrc.play.audit.EventKeys
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult._
import uk.gov.hmrc.play.audit.model.{DataEvent, EventTypes}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.time.DateTimeUtils

import scala.concurrent.{ExecutionContext, Future}


class BatchUpdaterSpec extends WordSpec with Matchers with MockFactory with ScalaFutures {

  override implicit def patienceConfig = PatienceConfig(timeout = scaled(Span(1, Second)))

  "The audited batch update service" should {

    implicit val ec = ExecutionContext.Implicits.global

    val id1 = ExampleID("1")
    val id2 = ExampleID("2")
    val id3 = ExampleID("3")

    val normalResult = BatchUpdateResult[ExampleID](
      tried = 3,
      succeeded = 0,
      alreadyUpdated = 0,
      invalidState = 0,
      notFound = List.empty,
      updateFailed = List.empty,
      auditFailed = List.empty
    )
    val auditDetails = Map("some" -> "Details")

    "Return empty results if given no IDs" in new TestCase {
      update(List.empty, action).futureValue should be (BatchUpdateResult.empty)
      `action.apply(...)`.verify(*, *).never()
    }

    "Handle and audit successful IDs" in new TestCase {
      `action.apply(...)`.when(*, *).returns(Future.successful(Succeeded(auditDetails)))
      `auditConnector.sendEvent(...)`.when(*, *, *).returns(Future.successful(Success))

      update(allIds, action).futureValue should be (normalResult.copy(succeeded = 3))

      for (id <- allIds) {
        `action.apply(...)`.verify(id, *)
        `auditConnector.sendEvent(...)`.verify(where(eventMatches(id, EventTypes.Succeeded, failureReason = None)))
      }
    }

    for ((status, expectedResult) <- Map(
      AlreadyUpdated(auditDetails) -> normalResult.copy(succeeded = 2, alreadyUpdated = 1),
      InvalidState(auditDetails) -> normalResult.copy(succeeded = 2, invalidState = 1),
      UpdateFailed(auditDetails) -> normalResult.copy(succeeded = 2, updateFailed = List(id2)),
      NotFound(auditDetails) -> normalResult.copy(succeeded = 2, notFound = List(id2)))) {
      s"Handle failing IDs with $status" in new TestCase {
        `action.apply(...)`.when(id1, *).returns(Future.successful(Succeeded(auditDetails)))
        `action.apply(...)`.when(id2, *).returns(Future.successful(status))
        `action.apply(...)`.when(id3, *).returns(Future.successful(Succeeded(auditDetails)))
        `auditConnector.sendEvent(...)`.when(*, *, *).returns(Future.successful(Success))

        update(allIds, action).futureValue should be(expectedResult)

        for (id <- allIds) `action.apply(...)`.verify(id, *)
        `auditConnector.sendEvent(...)`.verify(where(eventMatches(id2, EventTypes.Failed, failureReason = Some(status.failureReason.get))))
      }
    }

    "Handle and audit IDs where the action returns an exception" in new TestCase {
      `action.apply(...)`.when(id1, *).returns(Future.successful(Succeeded(auditDetails)))
      `action.apply(...)`.when(id2, *).throws(new RuntimeException("blah1"))
      `action.apply(...)`.when(id3, *).returns(Future.successful(Succeeded(auditDetails)))
      `auditConnector.sendEvent(...)`.when(*, *, *).returns(Future.successful(Success))

      update(allIds, action).futureValue should be (normalResult.copy(succeeded = 2, updateFailed = List(id2)))

      for (id <- allIds) `action.apply(...)`.verify(id, *)
      for (item <- List(id1, id3)) `auditConnector.sendEvent(...)`.verify(where(eventMatches(item, EventTypes.Succeeded, failureReason = None)))
      `auditConnector.sendEvent(...)`.verify(where(eventMatches(id2, EventTypes.Failed, failureReason = Some("updateFailed"), extraAuditDetails = Map.empty)))
    }

    "Handle and audit IDs where the action returns a failed future" in new TestCase {
      `action.apply(...)`.when(id1, *).returns(Future.successful(Succeeded(auditDetails)))
      `action.apply(...)`.when(id2, *).returns(Future.failed(new RuntimeException("blah1")))
      `action.apply(...)`.when(id3, *).returns(Future.successful(Succeeded(auditDetails)))
      `auditConnector.sendEvent(...)`.when(*, *, *).returns(Future.successful(Success))

      update(allIds, action).futureValue should be (normalResult.copy(succeeded = 2, updateFailed = List(id2)))

      for (id <- allIds) `action.apply(...)`.verify(id, *)
      for (item <- List(id1, id3)) `auditConnector.sendEvent(...)`.verify(where(eventMatches(item, EventTypes.Succeeded, failureReason = None)))
      `auditConnector.sendEvent(...)`.verify(where(eventMatches(id2, EventTypes.Failed, failureReason = Some("updateFailed"), extraAuditDetails = Map.empty)))
    }

    "Return a ID that succeeded but the audit failed" in new TestCase {
      `action.apply(...)`.when(*, *).returns(Future.successful(Succeeded(auditDetails)))
      `auditConnector.sendEvent(...)`.when(where(eventMatches(id1, EventTypes.Succeeded, failureReason = None))).returns(Future.successful(Success))
      `auditConnector.sendEvent(...)`.when(where(eventMatches(id2, EventTypes.Succeeded, failureReason = None))).returns(Future.successful(Disabled))
      `auditConnector.sendEvent(...)`.when(where(eventMatches(id3, EventTypes.Succeeded, failureReason = None))).returns(Future.failed(Failure("melons")))
      update(allIds, action).futureValue should be(normalResult.copy(succeeded = 3, auditFailed = List(id3)))
    }

    "Return a ID where the update didn't occur and the audit failed" in new TestCase {
      `action.apply(...)`.when(id1, *).returns(Future.successful(Succeeded(auditDetails)))
      `action.apply(...)`.when(id2, *).returns(Future.successful(UpdateFailed(auditDetails)))
      `action.apply(...)`.when(id3, *).returns(Future.successful(Succeeded(auditDetails)))
      `auditConnector.sendEvent(...)`.when(where(eventMatches(id1, EventTypes.Succeeded, failureReason = None))).returns(Future.successful(Success))
      `auditConnector.sendEvent(...)`.when(where(eventMatches(id2, EventTypes.Failed, failureReason = Some("updateFailed")))).returns(Future.failed(Failure("melons")))
      `auditConnector.sendEvent(...)`.when(where(eventMatches(id3, EventTypes.Succeeded, failureReason = None))).returns(Future.successful(Success))

      update(allIds, action).futureValue should be (normalResult.copy(succeeded = 2, updateFailed = List(id2), auditFailed = List(id2)))
    }

    trait TestCase extends BatchUpdater { testCase =>

      val allIds = List(id1, id2, id3)

      val now = DateTimeUtils.now

      override val auditConnector = stub[AuditConnector]
      val `auditConnector.sendEvent(...)` = toStubFunction3(auditConnector.sendEvent(_: DataEvent)(_: HeaderCarrier, _: ExecutionContext))

      val transactionName = "some trans name"
      val appName = "someAppName"
      val idName = "someIdName"

      def eventMatches(id: ExampleID, eventType: String, failureReason: Option[String], extraAuditDetails: Map[String, String] = auditDetails)(event: DataEvent, hc: HeaderCarrier, ec: ExecutionContext): Boolean = event match {
        case DataEvent(auditSource, auditType, _, tags, detail, _) =>
          auditSource == appName &&
            auditType == eventType &&
            tags == Map(EventKeys.TransactionName -> transactionName) &&
            detail == Map(idName -> id.stringify) ++ extraAuditDetails ++ failureReason.map(f => Map("failureReason" -> f)).getOrElse(Map())
        case _ => false
      }

      val action = stub[UpdateAction[ExampleID]]
      (action.transactionName _).when().returns(transactionName)

      val `action.apply(...)` = toStubFunction2(action.apply(_: ExampleID)(_: ExecutionContext))
    }
  }

}
