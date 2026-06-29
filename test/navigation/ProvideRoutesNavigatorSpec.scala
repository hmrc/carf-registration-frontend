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
import models.ProvideMode
import pages.changeContactDetails.*

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

    "when on ChangeDetailsOrgFirstNamePage" - {
      "must navigate to ChangeOrgFirstContactEmailController in ProvideMode" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgFirstNamePage, "Tax Team")
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgFirstNamePage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgFirstContactEmailController.onPageLoad(ProvideMode)
      }
    }

    "when on ChangeDetailsOrgFirstEmailPage" - {
      "must navigate to ChangeOrgFirstContactHavePhoneController in ProvideMode" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgFirstEmailPage, "tax@example.com")
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgFirstEmailPage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgFirstContactHavePhoneController.onPageLoad(ProvideMode)
      }
    }

    "when on ChangeDetailsOrgFirstHavePhonePage" - {
      "must navigate to ChangeOrgFirstContactPhoneNumberController in ProvideMode when answer is Yes" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgFirstHavePhonePage, true)
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgFirstHavePhonePage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgFirstContactPhoneNumberController.onPageLoad(ProvideMode)
      }

      "must navigate to ChangeOrgHaveSecondContactController in ProvideMode when answer is No" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgFirstHavePhonePage, false)
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgFirstHavePhonePage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgHaveSecondContactController.onPageLoad(ProvideMode)
      }

      "must navigate to JourneyRecoveryController when answer is missing" in {
        navigator.nextPage(
          ChangeDetailsOrgFirstHavePhonePage,
          ProvideMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on ChangeDetailsOrgFirstPhoneNumberPage" - {
      "must navigate to ChangeOrgHaveSecondContactController in ProvideMode" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgFirstPhoneNumberPage, "07111111111")
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgFirstPhoneNumberPage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgHaveSecondContactController.onPageLoad(ProvideMode)
      }
    }

    "when on ChangeDetailsOrgHaveSecondContactPage" - {
      "must navigate to ChangeOrgSecondContactNameController in ProvideMode when answer is Yes" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgHaveSecondContactPage, true)
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgHaveSecondContactPage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgSecondContactNameController.onPageLoad(ProvideMode)
      }

      "must navigate to ChangeOrganisationContactDetailsController when answer is No" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgHaveSecondContactPage, false)
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgHaveSecondContactPage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrganisationContactDetailsController.onPageLoad()
      }

      "must navigate to JourneyRecoveryController when answer is missing" in {
        navigator.nextPage(
          ChangeDetailsOrgHaveSecondContactPage,
          ProvideMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
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

    "when on ChangeDetailsOrgSecondEmailPage" - {
      "must navigate to ChangeOrgSecondContactHavePhoneController in ProvideMode" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgSecondEmailPage, "test@example.com")
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgSecondEmailPage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgSecondContactHavePhoneController.onPageLoad(ProvideMode)
      }
    }

    "when on ChangeDetailsOrgSecondHavePhonePage" - {
      "must navigate to ChangeOrgSecondContactPhoneNumberController in ProvideMode when answer is Yes" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgSecondHavePhonePage, true)
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgSecondHavePhonePage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrgSecondContactPhoneNumberController.onPageLoad(ProvideMode)
      }

      "must navigate to ChangeOrganisationContactDetailsController when answer is No" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgSecondHavePhonePage, false)
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgSecondHavePhonePage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrganisationContactDetailsController.onPageLoad()
      }

      "must navigate to JourneyRecoveryController when answer is missing" in {
        navigator.nextPage(
          ChangeDetailsOrgSecondHavePhonePage,
          ProvideMode,
          emptyUserAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "when on ChangeDetailsOrgSecondPhoneNumberPage" - {
      "must navigate to ChangeOrganisationContactDetailsController" in {
        val userAnswers = emptyUserAnswers
          .set(ChangeDetailsOrgSecondPhoneNumberPage, "07222222222")
          .success
          .value
        navigator.nextPage(
          ChangeDetailsOrgSecondPhoneNumberPage,
          ProvideMode,
          userAnswers
        ) mustBe changeDetailsRoutes.ChangeOrganisationContactDetailsController.onPageLoad()
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
