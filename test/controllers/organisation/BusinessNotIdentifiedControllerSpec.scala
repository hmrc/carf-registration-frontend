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
import controllers.routes
import models.RegistrationType.{LimitedCompany, SoleTrader}
import models.{OrganisationRegistrationType, RegistrationType, UniqueTaxpayerReference, UserAnswers}
import pages.organisation.{RegistrationTypePage, UniqueTaxpayerReferenceInUserAnswers, WhatIsTheNameOfYourBusinessPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.BusinessNotIdentifiedView

class BusinessNotIdentifiedControllerSpec extends SpecBase {

  val testBusinessName                             = "Test Corp"
  val testRegType: RegistrationType                = LimitedCompany
  val testOrgRegType: OrganisationRegistrationType = OrganisationRegistrationType.OrganisationLimitedCompany

  val testCompaniesHouseSearchUrl: String = "https://find-and-update.company-information.service.gov.uk/"
  val testRegistrationStartUrl: String    = "/register-for-carf"
  val testFindUTRUrl: String              = "https://www.gov.uk/find-utr-number"
  val testAeoiEmailAddress: String        = "aeoi.enquiries@hmrc.gov.uk"

  val userAnswersWithData: UserAnswers = emptyUserAnswers
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrString))
    .success
    .value
    .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
    .success
    .value
    .set(RegistrationTypePage, testRegType)
    .success
    .value

  "BusinessNotIdentified Controller" - {

    "must return OK and the correct view for a GET when required data is present" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithData)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.organisation.routes.BusinessNotIdentifiedController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[BusinessNotIdentifiedView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(
          utr = testUtrString,
          businessName = testBusinessName,
          organisationType = testOrgRegType,
          companiesHouseSearchUrl = testCompaniesHouseSearchUrl,
          registrationStartUrl = testRegistrationStartUrl,
          findUTRUrl = testFindUTRUrl,
          aeoiEmailAddress = testAeoiEmailAddress
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if UTR is not present" in {

      val userAnswersWithoutUtr = emptyUserAnswers
        .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
        .success
        .value
        .set(RegistrationTypePage, testRegType)
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
        .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrString))
        .success
        .value
        .set(RegistrationTypePage, testRegType)
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

    "must redirect to Journey Recovery for a GET if Business type is missing" in {

      val userAnswersWithoutName = emptyUserAnswers
        .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrString))
        .success
        .value
        .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
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

    "must redirect to Journey Recovery for a GET if Business type is Sole Trader" in {

      val userAnswersWithoutName = emptyUserAnswers
        .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrString))
        .success
        .value
        .set(WhatIsTheNameOfYourBusinessPage, testBusinessName)
        .success
        .value
        .set(RegistrationTypePage, SoleTrader)
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
