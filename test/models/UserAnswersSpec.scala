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
import pages.individual.*
import pages.organisation.*

class UserAnswersSpec extends SpecBase {

  "UserAnswers" - {
    "clearMatchFlagAndSafeId method" - {
      "should set match flag to false and remove the safe id when they are true and present" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result = ua.clearMatchFlagAndSafeId

        result.safeId        mustBe None
        result.hasValidMatch mustBe false
      }
      "should keep match flag to false and keep the safe id as removed when they are false and not present" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result = ua.clearMatchFlagAndSafeId

        result.safeId        mustBe None
        result.hasValidMatch mustBe false
      }
    }

    ".hasCompleteIndividualContactDetails" - {
      "when no individual contact details are provided" in {
        val ua = emptyUserAnswers

        ua.hasCompleteIndividualContactDetails mustBe false
      }

      "when only individual email has been provided" in {
        val ua = emptyUserAnswers.withPage(IndividualEmailPage, testEmail)

        ua.hasCompleteIndividualContactDetails mustBe false
      }

      "when only individual email has been provided and havePhone is false" in {
        val ua = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, false)

        ua.hasCompleteIndividualContactDetails mustBe true
      }

      "when only individual email has been provided and havePhone is true but phone number is not provided" in {
        val ua = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)

        ua.hasCompleteIndividualContactDetails mustBe false
      }

      "when only individual email has been provided and havePhone is true and phone number is provided" in {
        val ua = emptyUserAnswers
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, true)
          .withPage(IndividualPhoneNumberPage, testPhone)

        ua.hasCompleteIndividualContactDetails mustBe true
      }
    }

    ".hasCompleteOrganisationContactDetails" - {
      "when no organisation contact details are provided" in {
        val ua = emptyUserAnswers

        ua.hasCompleteOrganisationContactDetails mustBe false
      }

      "when only first contact name is provided" in {
        val ua = emptyUserAnswers.withPage(FirstContactNamePage, testName)

        ua.hasCompleteOrganisationContactDetails mustBe false
      }

      "when only first contact name and email are provided" in {
        val ua = emptyUserAnswers
          .withPage(FirstContactNamePage, testName)
          .withPage(FirstContactEmailPage, testEmail)

        ua.hasCompleteOrganisationContactDetails mustBe false
      }

      "when first contact details are complete but haveSecondContact is not answered" in {
        val ua = emptyUserAnswers
          .withPage(FirstContactNamePage, testName)
          .withPage(FirstContactEmailPage, testEmail)
          .withPage(FirstContactPhonePage, false)

        ua.hasCompleteOrganisationContactDetails mustBe false
      }

      "when first contact details are missing phone number and haveSecondContact is false" in {
        val ua = emptyUserAnswers
          .withPage(FirstContactNamePage, testName)
          .withPage(FirstContactEmailPage, testEmail)
          .withPage(FirstContactPhonePage, true)
          .withPage(OrganisationHaveSecondContactPage, false)

        ua.hasCompleteOrganisationContactDetails mustBe false
      }

      "when first contact details are complete (firstContactHavePhone is false) and haveSecondContact is false" in {
        val ua = emptyUserAnswers
          .withPage(FirstContactNamePage, testName)
          .withPage(FirstContactEmailPage, testEmail)
          .withPage(FirstContactPhonePage, false)
          .withPage(OrganisationHaveSecondContactPage, false)

        ua.hasCompleteOrganisationContactDetails mustBe true
      }

      "when first contact details are complete (firstContactHavePhone is true) and haveSecondContact is false" in {
        val ua = emptyUserAnswers
          .withPage(FirstContactNamePage, testName)
          .withPage(FirstContactEmailPage, testEmail)
          .withPage(FirstContactPhonePage, true)
          .withPage(FirstContactPhoneNumberPage, testPhone)
          .withPage(OrganisationHaveSecondContactPage, false)

        ua.hasCompleteOrganisationContactDetails mustBe true
      }

      "when first contact details are complete and haveSecondContact is true" - {
        "when no second contact details are provided" in {
          val ua = emptyUserAnswers
            .withPage(FirstContactNamePage, testName)
            .withPage(FirstContactEmailPage, testEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)

          ua.hasCompleteOrganisationContactDetails mustBe false
        }

        "when only second contact name is provided" in {
          val ua = emptyUserAnswers
            .withPage(FirstContactNamePage, testName)
            .withPage(FirstContactEmailPage, testEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testName)

          ua.hasCompleteOrganisationContactDetails mustBe false
        }

        "when only second contact name and email are provided" in {
          val ua = emptyUserAnswers
            .withPage(FirstContactNamePage, testName)
            .withPage(FirstContactEmailPage, testEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)

          ua.hasCompleteOrganisationContactDetails mustBe false
        }

        "when second contact details are complete (secondContactHavePhone is false)" in {
          val ua = emptyUserAnswers
            .withPage(FirstContactNamePage, testName)
            .withPage(FirstContactEmailPage, testEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, false)

          ua.hasCompleteOrganisationContactDetails mustBe true
        }

        "when second contact details are complete (secondContactHavePhone is true)" in {
          val ua = emptyUserAnswers
            .withPage(FirstContactNamePage, testName)
            .withPage(FirstContactEmailPage, testEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)
            .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

          ua.hasCompleteOrganisationContactDetails mustBe true
        }

        "when secondContactHavePhone is true but phone number is missing" in {
          val ua = emptyUserAnswers
            .withPage(FirstContactNamePage, testName)
            .withPage(FirstContactEmailPage, testEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testPhone)
            .withPage(OrganisationHaveSecondContactPage, true)
            .withPage(OrganisationSecondContactNamePage, testName)
            .withPage(OrganisationSecondContactEmailPage, testEmail)
            .withPage(OrganisationSecondContactHavePhonePage, true)

          ua.hasCompleteOrganisationContactDetails mustBe false
        }
      }
    }
  }
}
