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

package controllers.individual

import base.SpecBase
import controllers.routes
import models.{Name, UniqueTaxpayerReference, UserAnswers}
import pages.organisation.{WhatIsYourNamePage, YourUniqueTaxpayerReferencePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.individual.ProblemSoleTraderNotIdentifiedView

class ProblemSoleTraderNotIdentifiedControllerSpec extends SpecBase {

  val testUtrString: String = "1234567890"
  val testName              = "Timmy Turnips"

  val testIndexPageUrl: String     = controllers.routes.IndexController.onPageLoad().url
  val guidancePageUrl: String      = "https://www.gov.uk/find-utr-number"
  val testAeoiEmailAddress: String = "aeoi.enquiries@hmrc.gov.uk"

  "ProblemSoleTraderNotIdentified Controller" - {

    "must return OK and the correct view for a GET when required data is present" in {

      val userAnswersWithData: UserAnswers = emptyUserAnswers
        .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference(testUtrString))
        .success
        .value
        .set(WhatIsYourNamePage, Name(firstName = "Timmy", lastName = "Turnips"))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ProblemSoleTraderNotIdentifiedView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          testUtrString,
          testName,
          testIndexPageUrl,
          guidancePageUrl,
          testAeoiEmailAddress
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if UTR is not present" in {

      val userAnswersNoUtr: UserAnswers = emptyUserAnswers
        .set(WhatIsYourNamePage, Name(firstName = "Timmy", lastName = "Turnips"))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswersNoUtr)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad().url)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if Name is not present" in {

      val userAnswersNoName: UserAnswers = emptyUserAnswers
        .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference(uniqueTaxPayerReference = testUtrString))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswersNoName)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.individual.routes.ProblemSoleTraderNotIdentifiedController.onPageLoad().url)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
