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

package navigation

import base.SpecBase
import controllers.changeContactDetails.routes as changeDetailsRoutes
import controllers.routes
import models.{ProvideMode, UserAnswers}
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage, ChangeDetailsOrgSecondEmailPage, ChangeDetailsOrgSecondNamePage}
import pages.individual.HaveNiNumberPage

class ProvideRoutesNavigatorSpec extends SpecBase {

  val navigator = new Navigator()

  "ProvideRoutesNavigator" - {

    "when on ChangeDetailsIndividualEmailPage" - {
      "must navigate to ChangeDetailsIndividualHavePhoneController in ProvideMode" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsIndividualEmailPage, "test@example.com")
          .success
          .value

        navigator.nextPage(
          ChangeDetailsIndividualEmailPage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeDetailsIndividualHavePhoneController.onPageLoad(ProvideMode)
      }
    }

    "when on ChangeDetailsIndividualHavePhonePage" - {
      "must navigate to ChangeIndividualPhoneNumberController in ProvideMode when answer is Yes" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsIndividualHavePhonePage, true)
          .success
          .value

        navigator.nextPage(
          ChangeDetailsIndividualHavePhonePage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeIndividualPhoneNumberController.onPageLoad(ProvideMode)
      }

      "must navigate to ChangeIndividualContactDetailsController when answer is No" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsIndividualHavePhonePage, false)
          .success
          .value

        navigator.nextPage(
          ChangeDetailsIndividualHavePhonePage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeIndividualContactDetailsController.onPageLoad()
      }

      "must navigate to JourneyRecoveryController when answer is missing" in {
        val userAnswers = emptyUserAnswers

        navigator.nextPage(
          ChangeDetailsIndividualHavePhonePage,
          ProvideMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on ChangeDetailsIndividualPhoneNumberPage" - {
      "must navigate to ChangeIndividualContactDetailsController" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsIndividualPhoneNumberPage, "07777777777")
          .success
          .value

        navigator.nextPage(
          ChangeDetailsIndividualPhoneNumberPage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeIndividualContactDetailsController.onPageLoad()
      }
    }

    "when on ChangeDetailsOrgSecondEmailPage" - {
      "must navigate to ChangeDetailsOrganisationSecondContactHavePhoneController in ProvideMode" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgSecondEmailPage, "test@example.com")
          .success
          .value

        navigator.nextPage(
          ChangeDetailsOrgSecondEmailPage,
          ProvideMode,
          userAnswers
        ) mustBe routes.PlaceholderController.onPageLoad(
          "Should redirect to change-contact/organisation/second-contact-have-phone page (CARF-192)"
        )
      }
    }

    "when on ChangeDetailsOrgSecondNamePage" - {
      "must navigate to ChangeOrgSecondContactEmailController in ProvideMode" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgSecondNamePage, "Sandy Barnes")
          .success
          .value

        navigator.nextPage(
          ChangeDetailsOrgSecondNamePage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgSecondContactEmailController.onPageLoad(ProvideMode)
      }
    }

    "when on an unknown page" - {
      "must navigate to JourneyRecoveryController" in {
        val userAnswers = emptyUserAnswers

        navigator.nextPage(
          pages.individual.HaveNiNumberPage,
          ProvideMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }
}
