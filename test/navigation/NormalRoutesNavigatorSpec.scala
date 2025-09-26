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
import models.{NormalMode, UserAnswers}
import pages.{OrganisationRegistrationTypePage, Page, RegisteredAddressInUkPage}

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
  }

  "RegisteredAddressInUkPage navigation" - {

    "must go to UTR page when user answers 'Yes' to UK address" in {
      val userAnswers = UserAnswers("id").set(RegisteredAddressInUkPage, true).success.value

      navigator.nextPage(
        RegisteredAddressInUkPage,
        NormalMode,
        userAnswers
      ) mustBe routes.PlaceholderController.onPageLoad(
        "Must redirect to /register/utr (What is your UTR page - CARF-122)"
      )
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
  }
}
