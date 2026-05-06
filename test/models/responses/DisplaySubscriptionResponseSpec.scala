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

package models.responses

import base.SpecBase

class DisplaySubscriptionResponseSpec extends SpecBase {

  "DisplaySubscriptionResponse model" - {
    "isIndividualRegistrationType val" - {
      "must return true when only individual primary contact is present" in {
        val result = testIndividualDisplaySubscriptionResponse(true).isIndividualRegistrationType

        result mustBe Some(true)
      }
      "must return false when only organisation primary contact is present" in {
        val result = testOrganisationDisplaySubscriptionResponse.isIndividualRegistrationType

        result mustBe Some(false)
      }
      "must return None when no primary contact is present" in {
        val result = testInvalidDisplaySubscriptionResponse.isIndividualRegistrationType

        result mustBe None
      }
      "must return None when no both primary contact types are present" in {
        val result = testInvalidDisplaySubscriptionResponseBoth.isIndividualRegistrationType

        result mustBe None
      }
    }
  }

  "DisplaySubscriptionResponse extension method hasIndividualChangedData" - {
    "must return true if email changed" in {
      val result = testIndividualDisplaySubscriptionResponse(true)
        .hasIndividualChangedData(email = "DIFF EMAIL", phone = Some(testPhone))

      result mustBe true
    }
    "must return true if phone changed" in {
      val result = testIndividualDisplaySubscriptionResponse(true)
        .hasIndividualChangedData(email = testEmail, phone = Some("DIFF PHONE"))

      result mustBe true
    }
    "must return true if phone is None" in {
      val result =
        testIndividualDisplaySubscriptionResponse(true).hasIndividualChangedData(email = testEmail, phone = None)

      result mustBe true
    }
    "must return true if email and phone changed" in {
      val result = testIndividualDisplaySubscriptionResponse(true)
        .hasIndividualChangedData(email = "DIFF EMAIL", phone = Some("DIFF PHONE"))

      result mustBe true
    }
    "must return false if nothing changed" in {
      val result = testIndividualDisplaySubscriptionResponse(true)
        .hasIndividualChangedData(email = testEmail, phone = Some(testPhone))

      result mustBe false
    }
  }

  "DisplaySubscriptionResponse extension method hasOrganisationChangedData" - {
    "must return true if email changed" in {
      val result = testOrgDisplaySubscriptionResponseWithSecondaryNoPhone
        .hasOrganisationChangedData(
          firstContactEmail = "DIFF Email",
          firstContactName = "Bobby",
          firstContactPhone = Some(testPhone),
          secondContactName = Some("Bobby"),
          secondContactEmail = Some(testEmail),
          secondContactPhone = Some(testPhone)
        )

      result mustBe true
    }
    "must return true if phone changed" in {
      val result = testOrgDisplaySubscriptionResponseWithSecondaryNoPhone
        .hasOrganisationChangedData(
          firstContactEmail = testEmail,
          firstContactName = "Bobby",
          firstContactPhone = Some("DIFF PHONE"),
          secondContactName = Some("Bobby"),
          secondContactEmail = Some(testEmail),
          secondContactPhone = Some(testPhone)
        )

      result mustBe true
    }
    "must return true if phone is None" in {
      val result =
        testOrgDisplaySubscriptionResponseWithSecondaryNoPhone
          .hasOrganisationChangedData(
            firstContactEmail = testEmail,
            firstContactName = "Bobby",
            firstContactPhone = None,
            secondContactName = Some("Bobby"),
            secondContactEmail = Some(testEmail),
            secondContactPhone = None
          )

      result mustBe true
    }
    "must return true if email and phone changed" in {
      val result = testOrgDisplaySubscriptionResponseWithSecondaryNoPhone
        .hasOrganisationChangedData(
          firstContactEmail = "DIFF EMAIL",
          firstContactName = "Bobby",
          firstContactPhone = Some("DIFF PHONE"),
          secondContactName = Some("Bobby"),
          secondContactEmail = Some(testEmail),
          secondContactPhone = None
        )

      result mustBe true
    }
    "must return false if nothing changed" in {
      val result = testOrgDisplaySubscriptionResponseWithSecondaryNoPhone
        .hasOrganisationChangedData(
          firstContactEmail = testEmail,
          firstContactName = "Bobby",
          firstContactPhone = Some(testPhone),
          secondContactName = Some("Bobby"),
          secondContactEmail = Some(testEmail),
          secondContactPhone = None
        )

      result mustBe false
    }
    "must return true if secondary email changed" in {
      val result = testOrgDisplaySubscriptionResponseWithSecondaryNoPhone
        .hasOrganisationChangedData(
          firstContactEmail = testEmail,
          firstContactName = "Bobby",
          firstContactPhone = Some(testPhone),
          secondContactName = Some("Bobby"),
          secondContactEmail = Some("DIFF Email"),
          secondContactPhone = Some(testPhone)
        )

      result mustBe true
    }
    "must return true if secondary phone changed" in {
      val result = testOrgDisplaySubscriptionResponseWithSecondaryNoPhone
        .hasOrganisationChangedData(
          firstContactEmail = testEmail,
          firstContactName = "Bobby",
          firstContactPhone = Some(testPhone),
          secondContactName = Some("Bobby"),
          secondContactEmail = Some(testEmail),
          secondContactPhone = Some(testPhone)
        )

      result mustBe true
    }
    "must return true if secondary email and phone changed" in {
      val result = testOrgDisplaySubscriptionResponseWithSecondaryNoPhone
        .hasOrganisationChangedData(
          firstContactEmail = testEmail,
          firstContactName = "Bobby",
          firstContactPhone = Some("DIFF PHONE"),
          secondContactName = Some("Bobby"),
          secondContactEmail = Some("DIFF EMAIL"),
          secondContactPhone = Some(testPhone)
        )

      result mustBe true
    }
  }

}
