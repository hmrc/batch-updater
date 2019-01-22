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

sealed trait SingleResult {
  val failureReason: Option[String]
  val auditDetails: Map[String,String]
}
object SingleResult {
  case class Succeeded(auditDetails: Map[String, String])       extends SingleResult { val failureReason = None }
  case class AlreadyUpdated(auditDetails: Map[String, String])  extends SingleResult { val failureReason = Some("alreadyUpdated") }
  case class InvalidState(auditDetails: Map[String, String])    extends SingleResult { val failureReason = Some("invalidState") }
  case class NotFound(auditDetails: Map[String, String])        extends SingleResult { val failureReason = Some("notFound") }
  case class UpdateFailed(auditDetails: Map[String, String])    extends SingleResult { val failureReason = Some("updateFailed") }
  case object AuditFailed                                       extends SingleResult {
    val failureReason = Some("auditFailed")
    val auditDetails = Map.empty[String, String]
  }

  object Succeeded      { def apply(): Succeeded      = Succeeded(Map.empty) }
  object AlreadyUpdated { def apply(): AlreadyUpdated = AlreadyUpdated(Map.empty) }
  object InvalidState   { def apply(): InvalidState   = InvalidState(Map.empty) }
  object NotFound       { def apply(): NotFound       = NotFound(Map.empty) }
  object UpdateFailed   { def apply(): UpdateFailed   = UpdateFailed(Map.empty) }
}
