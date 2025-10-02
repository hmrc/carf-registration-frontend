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
import pages.{HaveNiNumberPage, OrganisationRegistrationTypePage, Page, RegisteredAddressInUkPage, YourUniqueTaxpayerReferencePage}

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
      ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /business-name")
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
      ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /your-name")
    }

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
      ) mustBe routes.PlaceholderController.onPageLoad(
        "Must redirect to /register/have-utr (Do you have a UTR page - CARF-123)"
      )
    }

    "must go to Journey Recovery when no answer is provided" in {
      val userAnswers = UserAnswers("id")

      navigator.nextPage(
        RegisteredAddressInUkPage,
        NormalMode,
        userAnswers
      ) mustBe routes.JourneyRecoveryController.onPageLoad()
    }

    "HaveNiNumberPage navigation" - {
      "when user answers 'true' (yes, I have a National Insurance number)" - {
        "must navigate to: What is your National Insurance number?" in {
          val userAnswers = UserAnswers("id")
            .set(HaveNiNumberPage, true)
            .success
            .value

          navigator.nextPage(
            HaveNiNumberPage,
            NormalMode,
            userAnswers
          ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /ni-number (CARF-164)")
        }
      }
      "when user answers 'false' (no, I don't have a National Insurance number)" - {
        "must navigate to: What is your name?" in {
          val userAnswers = UserAnswers("id")
            .set(HaveNiNumberPage, false)
            .success
            .value

          navigator.nextPage(
            HaveNiNumberPage,
            NormalMode,
            userAnswers
          ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /without-id/name (CARF-169)")
        }
      }
    }
  }
}
