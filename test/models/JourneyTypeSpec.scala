/*
 * Copyright 2025 HM Revenue & Customs
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

package models

import base.SpecBase
import play.api.libs.json.*

class JourneyTypeSpec extends SpecBase {

  "JourneyType Reads" - {

    "read OrgWithUtr from JSON" in {
      val json = JsString("OrgWithUtr")

      json.validate[JourneyType] mustBe JsSuccess(OrgWithUtr)
    }

    "read OrgWithoutId from JSON" in {
      val json = JsString("OrgWithoutId")

      json.validate[JourneyType] mustBe JsSuccess(OrgWithoutId)
    }

    "read IndWithNino from JSON" in {
      val json = JsString("IndWithNino")

      json.validate[JourneyType] mustBe JsSuccess(IndWithNino)
    }

    "read IndWithUtr from JSON" in {
      val json = JsString("IndWithUtr")

      json.validate[JourneyType] mustBe JsSuccess(IndWithUtr)
    }

    "read IndWithoutId from JSON" in {
      val json = JsString("IndWithoutId")

      json.validate[JourneyType] mustBe JsSuccess(IndWithoutId)
    }

    "fail when an unknown JourneyType is provided" in {
      val json = JsString("SomethingElse")

      json.validate[JourneyType] mustBe JsError("Unknown JourneyType: SomethingElse")
    }

    "fail when JSON is not a string" in {
      val json = JsNumber(1)

      json.validate[JourneyType] mustBe JsError("JourneyType must be a string")
    }
  }

  "JourneyType Writes" - {

    "write OrgWithUtr to JSON" in {
      Json.toJson[JourneyType](OrgWithUtr) mustBe JsString("OrgWithUtr")
    }

    "write OrgWithoutId to JSON" in {
      Json.toJson[JourneyType](OrgWithoutId) mustBe JsString("OrgWithoutId")
    }

    "write IndWithNino to JSON" in {
      Json.toJson[JourneyType](IndWithNino) mustBe JsString("IndWithNino")
    }

    "write IndWithUtr to JSON" in {
      Json.toJson[JourneyType](IndWithUtr) mustBe JsString("IndWithUtr")
    }

    "write IndWithoutId to JSON" in {
      Json.toJson[JourneyType](IndWithoutId) mustBe JsString("IndWithoutId")
    }
  }

  "JourneyType Format" - {

    "round-trip OrgWithUtr successfully" in {
      val json = Json.toJson[JourneyType](OrgWithUtr)

      json.validate[JourneyType] mustBe JsSuccess(OrgWithUtr)
    }

    "round-trip OrgWithoutId successfully" in {
      val json = Json.toJson[JourneyType](OrgWithoutId)

      json.validate[JourneyType] mustBe JsSuccess(OrgWithoutId)
    }

    "round-trip IndWithNino successfully" in {
      val json = Json.toJson[JourneyType](IndWithNino)

      json.validate[JourneyType] mustBe JsSuccess(IndWithNino)
    }

    "round-trip IndWithUtr successfully" in {
      val json = Json.toJson[JourneyType](IndWithUtr)

      json.validate[JourneyType] mustBe JsSuccess(IndWithUtr)
    }

    "round-trip IndWithoutId successfully" in {
      val json = Json.toJson[JourneyType](IndWithoutId)

      json.validate[JourneyType] mustBe JsSuccess(IndWithoutId)
    }
  }
}
