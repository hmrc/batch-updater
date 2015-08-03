/*
 * Copyright 2015 HM Revenue & Customs
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

case class BatchUpdateResult[ID](tried: Int,
                           succeeded: Int,
                           alreadyUpdated: Int,
                           invalidState: Int,
                           messageNotFound: List[ID],
                           updateFailed: List[ID],
                           auditFailed: List[ID]) {

  def add(result: SingleResult, id: ID): BatchUpdateResult[ID] = result match {
    case SingleResult.Succeeded       => copy(tried = tried + 1, succeeded = succeeded + 1)
    case SingleResult.AlreadyUpdated  => copy(tried = tried + 1, alreadyUpdated = alreadyUpdated + 1)
    case SingleResult.InvalidState    => copy(tried = tried + 1, invalidState = invalidState + 1)
    case SingleResult.MessageNotFound => copy(tried = tried + 1, messageNotFound = messageNotFound :+ id)
    case SingleResult.UpdateFailed    => copy(tried = tried + 1, updateFailed = updateFailed :+ id)
    case SingleResult.AuditFailed     => copy(auditFailed = auditFailed :+ id)
  }
}

object BatchUpdateResult {
  def empty[ID] = BatchUpdateResult[ID](
    tried = 0,
    succeeded = 0,
    alreadyUpdated = 0,
    invalidState = 0,
    messageNotFound = List.empty,
    updateFailed = List.empty,
    auditFailed = List.empty
  )
}
