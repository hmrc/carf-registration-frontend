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
        ) mustBe Some(OrganisationRegistrationType.OrganisationSoleTrader)
      }

      "return LimitedCompany for LimitedCompany" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.LimitedCompany
        ) mustBe Some(OrganisationRegistrationType.OrganisationLimitedCompany)
      }

      "return Partnership for Partnership" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.Partnership
        ) mustBe Some(OrganisationRegistrationType.OrganisationPartnership)
      }

      "return LLP for LLP" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.LLP
        ) mustBe Some(OrganisationRegistrationType.OrganisationLLP)
      }

      "return Trust for Trust" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.Trust
        ) mustBe Some(OrganisationRegistrationType.OrganisationTrust)
      }

      "return None for Individual" in {
        OrganisationRegistrationType.fromRegistrationType(
          RegistrationType.Individual
        ) mustBe None
      }
    }
  }
}
