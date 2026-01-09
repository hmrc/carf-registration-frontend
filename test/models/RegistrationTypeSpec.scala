/*
 * Copyright 2026 HM Revenue & Customs
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

class RegistrationTypeSpec extends SpecBase {

  "RegistrationType Reads" - {

    "read LimitedCompany successfully" in {
      JsString("LimitedCompany").validate[RegistrationType] mustBe
        JsSuccess(RegistrationType.LimitedCompany)
    }

    "read Partnership successfully" in {
      JsString("Partnership").validate[RegistrationType] mustBe
        JsSuccess(RegistrationType.Partnership)
    }

    "read LLP successfully" in {
      JsString("LLP").validate[RegistrationType] mustBe
        JsSuccess(RegistrationType.LLP)
    }

    "read Trust successfully" in {
      JsString("Trust").validate[RegistrationType] mustBe
        JsSuccess(RegistrationType.Trust)
    }

    "read SoleTrader successfully" in {
      JsString("SoleTrader").validate[RegistrationType] mustBe
        JsSuccess(RegistrationType.SoleTrader)
    }

    "read Individual successfully" in {
      JsString("Individual").validate[RegistrationType] mustBe
        JsSuccess(RegistrationType.Individual)
    }

    "fail for an unknown value" in {
      JsString("SomethingElse").validate[RegistrationType] mustBe
        JsError("Unknown RegistrationType: SomethingElse")
    }

    "fail when JSON is not a string" in {
      JsNumber(1).validate[RegistrationType] mustBe
        JsError("RegistrationType must be a string")
    }
  }

  "RegistrationType Writes" - {

    "write LimitedCompany as a JSON string" in {
      Json.toJson(RegistrationType.LimitedCompany) mustBe
        JsString("LimitedCompany")
    }

    "write Partnership as a JSON string" in {
      Json.toJson(RegistrationType.Partnership) mustBe
        JsString("Partnership")
    }

    "write LLP as a JSON string" in {
      Json.toJson(RegistrationType.LLP) mustBe
        JsString("LLP")
    }

    "write Trust as a JSON string" in {
      Json.toJson(RegistrationType.Trust) mustBe
        JsString("Trust")
    }

    "write SoleTrader as a JSON string" in {
      Json.toJson(RegistrationType.SoleTrader) mustBe
        JsString("SoleTrader")
    }

    "write Individual as a JSON string" in {
      Json.toJson(RegistrationType.Individual) mustBe
        JsString("Individual")
    }
  }
}
