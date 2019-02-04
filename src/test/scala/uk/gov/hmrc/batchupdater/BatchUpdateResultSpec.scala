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

import org.scalatest.{WordSpec, Matchers}
import play.api.libs.json.Json

class BatchUpdateResultSpec extends WordSpec with Matchers {
  "BatchUpdateResult formatter" should {

    implicit val idFormat = Json.format[ExampleID]

    "Serialize properly when empty" in {
      Json.toJson(BatchUpdateResult.empty[ExampleID]) should be (Json.parse( """
                                                                               |{
                                                                               | "tried": 0,
                                                                               | "succeeded": 0,
                                                                               | "alreadyUpdated": 0,
                                                                               | "invalidState": 0,
                                                                               | "notFound": [],
                                                                               | "updateFailed": [],
                                                                               | "auditFailed": []
                                                                               |}
                                                                               |""".stripMargin))
    }
    "Serialize properly with values" in {
      val updateResult = BatchUpdateResult(
        tried = 1,
        succeeded = 2,
        alreadyUpdated = 3,
        invalidState = 4,
        notFound = List(ExampleID("a"), ExampleID("b")),
        updateFailed = List(ExampleID("c"), ExampleID("d")),
        auditFailed = List(ExampleID("e"), ExampleID("f")))
      Json.toJson(updateResult) should be (Json.parse("""|{
                                                         | "tried": 1,
                                                         | "succeeded": 2,
                                                         | "alreadyUpdated": 3,
                                                         | "invalidState": 4,
                                                         | "notFound": [{"value":"a"},{"value":"b"}],
                                                         | "updateFailed": [{"value":"c"},{"value":"d"}],
                                                         | "auditFailed": [{"value":"e"},{"value":"f"}]
                                                         |}
                                                         |""".stripMargin))
    }
  }
}
