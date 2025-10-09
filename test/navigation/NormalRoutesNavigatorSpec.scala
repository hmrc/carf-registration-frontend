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

package navigation

import base.SpecBase
import controllers.routes
import models.{NormalMode, OrganisationRegistrationType, UniqueTaxpayerReference, UserAnswers}
import pages.{HaveUTRPage, OrganisationRegistrationTypePage, Page, RegisteredAddressInUkPage, YourUniqueTaxpayerReferencePage}

class NormalRoutesNavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "NormalRoutesNavigator" - {

    "must go from a page that doesn't exist in the route map to Journey Recovery" in {

      case object UnknownPage extends Page
      navigator.nextPage(UnknownPage, NormalMode, UserAnswers("id")) mustBe routes.JourneyRecoveryController
        .onPageLoad()
    }

    "must go from OrganisationRegistrationTypePage to Registered Address in the UK page" in {

      navigator.nextPage(
        OrganisationRegistrationTypePage,
        NormalMode,
        UserAnswers("id")
      ) mustBe routes.RegisteredAddressInUkController.onPageLoad(NormalMode)
    }

    "must go from YourUniqueTaxpayerReferencePage to What is the registered name of your business for non soleTrader" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LimitedCompany)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        YourUniqueTaxpayerReferencePage,
        NormalMode,
        updatedAnswers
      ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /business-name (CARF-211)")
    }

    "must go from YourUniqueTaxpayerReferencePage to What is your name page for soleTrader" in {

      val updatedAnswers =
        emptyUserAnswers
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.SoleTrader)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference("1234567890"))
          .success
          .value

      navigator.nextPage(
        YourUniqueTaxpayerReferencePage,
        NormalMode,
        updatedAnswers
      ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /your-name (CARF-125)")
    }

    "RegisteredAddressInUkPage navigation" - {

      "must go to UTR page when user answers 'Yes' to UK address" in {
        val userAnswers = UserAnswers("id").set(RegisteredAddressInUkPage, true).success.value

        navigator.nextPage(
          RegisteredAddressInUkPage,
          NormalMode,
          userAnswers
        ) mustBe routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      }

      "must go to Have UTR page when user answers 'No' to UK address" in {
        val userAnswers = UserAnswers("id").set(RegisteredAddressInUkPage, false).success.value

        navigator.nextPage(
          RegisteredAddressInUkPage,
          NormalMode,
          userAnswers
        ) mustBe routes.HaveUTRController.onPageLoad(NormalMode)
      }

      "must go to Journey Recovery when no answer is provided" in {
        val userAnswers = UserAnswers("id")

        navigator.nextPage(
          RegisteredAddressInUkPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }

    "HaveUTRPage navigation" - {

      "must go to UTR page when user answers 'Yes' to having UTR" in {
        val userAnswers = UserAnswers("id").set(HaveUTRPage, true).success.value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      }

      "must go to Have NI Number page when user answers 'No' to having UTR and is SoleTrader" in {
        val userAnswers = UserAnswers("id")
          .set(HaveUTRPage, false)
          .success
          .value
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.SoleTrader)
          .success
          .value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe routes.PlaceholderController.onPageLoad(
          "redirect to - Do you have a National Insurance number? page /register/have-ni-number (CARF-163)"
        )
      }

      "must go to Business Name page when user answers 'No' to having UTR and is Organisation" in {
        val userAnswers = UserAnswers("id")
          .set(HaveUTRPage, false)
          .success
          .value
          .set(OrganisationRegistrationTypePage, OrganisationRegistrationType.LimitedCompany)
          .success
          .value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe routes.PlaceholderController.onPageLoad(
          "redirect to - What is the name of your business? page /register/without-id/business-name (CARF-148)"
        )
      }

      "must go to Journey Recovery when user answers 'No' to having UTR but no registration type is set" in {
        val userAnswers = UserAnswers("id").set(HaveUTRPage, false).success.value

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }

      "must go to Journey Recovery when no answer is provided for HaveUTR" in {
        val userAnswers = UserAnswers("id")

        navigator.nextPage(
          HaveUTRPage,
          NormalMode,
          userAnswers
        ) mustBe routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }
}
