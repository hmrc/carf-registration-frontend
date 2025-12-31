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
import play.api.libs.json.{JsString, Json}

class OrganisationRegistrationTypeSpec extends SpecBase {

  "OrganisationRegistration" - {
    "fromRegistrationType" - {
      "return SoleTrader for SoleTrader" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.SoleTrader
        ) mustBe Some(OrganisationRegistrationType.SoleTrader())
      }

      "return LimitedCompany for LimitedCompany" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.LimitedCompany
        ) mustBe Some(OrganisationRegistrationType.LimitedCompany())
      }

      "return Partnership for Partnership" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.Partnership
        ) mustBe Some(OrganisationRegistrationType.Partnership())
      }

      "return LLP for LLP" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.LLP
        ) mustBe Some(OrganisationRegistrationType.LLP())
      }

      "return Trust for Trust" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.Trust
        ) mustBe Some(OrganisationRegistrationType.Trust())
      }

      "return None for Individual" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.Individual
        ) mustBe None
      }
    }
    "writes" - {
      "write LimitedCompany as a JSON string" in {
        Json.toJson(OrganisationRegistrationType.LimitedCompany())(OrganisationRegistrationType.writes) mustBe
          JsString("LimitedCompany")
      }
      "write Partnership as a JSON string" in {
        Json.toJson(OrganisationRegistrationType.Partnership())(OrganisationRegistrationType.writes) mustBe
          JsString("Partnership")
      }
      "write LLP as a JSON string" in {
        Json.toJson(OrganisationRegistrationType.LLP())(OrganisationRegistrationType.writes) mustBe
          JsString("LLP")
      }
      "write Trust as a JSON string" in {
        Json.toJson(OrganisationRegistrationType.Trust())(OrganisationRegistrationType.writes) mustBe
          JsString("Trust")
      }
      "write SoleTrader as a JSON string" in {
        Json.toJson(OrganisationRegistrationType.SoleTrader())(OrganisationRegistrationType.writes) mustBe
          JsString("SoleTrader")
      }
    }
  }
}
