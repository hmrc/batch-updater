/*
 * Copyright 2020 HM Revenue & Customs
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

import ch.qos.logback.classic.{ Logger => LogbackLogger }
import org.slf4j.LoggerFactory
import play.api.libs.iteratee.{ Enumerator, Iteratee }
import uk.gov.hmrc.play.audit.EventKeys._
import uk.gov.hmrc.play.audit.http.connector.{ AuditConnector => Auditing }
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.audit.model.EventTypes.{ Failed => TxFailed, Succeeded => TxSucceeded }

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.reflectiveCalls

trait BatchUpdater {

  def auditConnector: Auditing
  def appName: String
  def idName: String
  val logger = LoggerFactory.getLogger(this.getClass).asInstanceOf[LogbackLogger]

  def update[ID](ids: List[ID], action: UpdateAction[ID])(
    implicit ec: ExecutionContext,
    stringify: Stringify[ID]): Future[BatchUpdateResult[ID]] = {
    def auditEvent(id: ID, result: SingleResult) = DataEvent(
      auditSource = appName,
      auditType = result.failureReason match {
        case Some(_) => TxFailed
        case None    => TxSucceeded
      },
      tags = Map(TransactionName -> action.transactionName),
      detail = Map(idName        -> stringify(id)) ++ result.failureReason
        .map(f => Map("failureReason" -> f))
        .getOrElse(Map()) ++ result.auditDetails
    )

    def sendAuditEvent(id: ID, result: SingleResult) = auditConnector.sendEvent(auditEvent(id, result))

    Enumerator.enumerate(ids) run Iteratee.foldM(BatchUpdateResult.empty[ID]) { (results, id) =>
      val resultF: Future[SingleResult] =
        try {
          action(id).recover {
            case e: Exception =>
              logger.warn(s"Failed ${action.transactionName} for ${stringify(id)}", e)
              SingleResult.UpdateFailed()
          }
        } catch {
          case e: Exception =>
            logger.warn(s"Failed ${action.transactionName} for ${stringify(id)}", e)
            Future.successful(SingleResult.UpdateFailed())
        }
      resultF.flatMap { r =>
        val batchResultsAfterAuditF =
          sendAuditEvent(id, r)
            .map(_ => results)
            .recover { case _ => results.add(SingleResult.AuditFailed, id) }

        batchResultsAfterAuditF.map(_.add(r, id))
      }
    }
  }
}
