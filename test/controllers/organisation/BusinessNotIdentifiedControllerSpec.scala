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

package controllers.organisation

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import models.{OrganisationRegistrationType, UniqueTaxpayerReference, UserAnswers}
import pages.organisation.{RegistrationTypePage, WhatIsTheNameOfYourBusinessPage, YourUniqueTaxpayerReferencePage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.BusinessNotIdentifiedView

class BusinessNotIdentifiedControllerSpec extends SpecBase {

  val testUtrString    = "1234567890"
  val testUtrObject    = UniqueTaxpayerReference(testUtrString)
  val testBusinessName = "Test Corp"
  val testOrgType      = OrganisationRegistrationType.LimitedCompany

  val userAnswersWithData: UserAnswers = emptyUserAnswers
    .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference(testUtrString))
    .success
    .value
    .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
    .success
    .value
    .set(RegistrationTypePage, testOrgType)
    .success
    .value

  "BusinessNotIdentified Controller" - {

    "must return OK and the correct view for a GET when required data is present" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad().url)

        val result = route(application, request).value

        val view      = application.injector.instanceOf[BusinessNotIdentifiedView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          testUtrString,
          testBusinessName,
          Some(testOrgType),
          appConfig
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if UTR is not present" in {

      val userAnswersWithoutUtr = emptyUserAnswers
        .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithoutUtr)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad().url)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if Business Name is not present" in {

      val userAnswersWithoutName = emptyUserAnswers
        .set(YourUniqueTaxpayerReferencePage, UniqueTaxpayerReference(testUtrString))
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithoutName)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad().url)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
