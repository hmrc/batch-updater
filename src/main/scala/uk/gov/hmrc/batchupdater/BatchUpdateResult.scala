/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.libs.json._
import play.api.libs.functional.syntax._
import SingleResult._

case class BatchUpdateResult[ID](tried: Int,
                                 succeeded: Int,
                                 alreadyUpdated: Int,
                                 invalidState: Int,
                                 notFound: List[ID],
                                 updateFailed: List[ID],
                                 auditFailed: List[ID]) {

  def add(result: SingleResult, id: ID): BatchUpdateResult[ID] = result match {
    case _: Succeeded       => copy(tried = tried + 1, succeeded = succeeded + 1)
    case _: AlreadyUpdated  => copy(tried = tried + 1, alreadyUpdated = alreadyUpdated + 1)
    case _: InvalidState    => copy(tried = tried + 1, invalidState = invalidState + 1)
    case _: NotFound        => copy(tried = tried + 1, notFound = notFound :+ id)
    case _: UpdateFailed    => copy(tried = tried + 1, updateFailed = updateFailed :+ id)
    case AuditFailed        => copy(auditFailed = auditFailed :+ id)
  }
}

object BatchUpdateResult {
  def empty[ID] = BatchUpdateResult[ID](
    tried = 0,
    succeeded = 0,
    alreadyUpdated = 0,
    invalidState = 0,
    notFound = List.empty,
    updateFailed = List.empty,
    auditFailed = List.empty
  )

  import scala.language.implicitConversions

  implicit def batchUpdateResultWrites[ID](implicit idFormat: Writes[ID]): Writes[BatchUpdateResult[ID]] = (
    (__ \ "tried").write[Int] and
    (__ \ "succeeded").write[Int] and
    (__ \ "alreadyUpdated").write[Int] and
    (__ \ "invalidState").write[Int] and
    (__ \ "notFound").write[List[ID]] and
    (__ \ "updateFailed").write[List[ID]] and
    (__ \ "auditFailed").write[List[ID]]
  )(unlift(BatchUpdateResult.unapply[ID] _))
}
