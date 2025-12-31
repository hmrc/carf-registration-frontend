package models

import base.SpecBase
import play.api.libs.json.{JsError, JsNumber, JsString, JsSuccess, Json}

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
