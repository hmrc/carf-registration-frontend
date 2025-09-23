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
import pages.{OrganisationRegistrationTypePage, Page, YourUniqueTaxpayerReferencePage}

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
      ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /registered-address-in-uk (CARF-121)")
    }

    "must go from YourUniqueTaxpayerReferencePage to What is the registered name of your business or Your name page" in {

      case object YourOrganisationNamePage extends Page
      navigator.nextPage(
        YourUniqueTaxpayerReferencePage,
        NormalMode,
        UserAnswers("id")
      ) mustBe routes.PlaceholderController.onPageLoad("Must redirect to /business-name or /your-name")
    }

  }

}
