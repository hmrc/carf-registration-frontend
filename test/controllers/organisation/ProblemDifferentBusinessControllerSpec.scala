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
import models.responses.AddressRegistrationResponse
import models.{BusinessDetails, IsThisYourBusinessPageDetails}
import pages.IsThisYourBusinessPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.ProblemDifferentBusinessView

class ProblemDifferentBusinessControllerSpec extends SpecBase {

  val testFullSignOutUrl: String =
    "http://localhost:9553/bas-gateway/sign-out-without-state?continue=http://localhost:17000/register-for-cryptoasset-reporting"

  val controllerRoute: String = controllers.organisation.routes.ProblemDifferentBusinessController.onPageLoad().url

  "ProblemDifferentBusiness Controller" - {
    "must return OK and not display country code for a GB address, & show correct view for a GET" in {
      val businessName = "Agent ABC Ltd"
      val address      = AddressRegistrationResponse(
        addressLine1 = "2 High Street",
        addressLine2 = Some("Birmingham"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("B23 2AZ"),
        countryCode = "GB"
      )
      val pageDetails  = IsThisYourBusinessPageDetails(
        businessDetails = BusinessDetails(name = businessName, address = address, safeId = testSafeId),
        pageAnswer = Some(true)
      )
      val userAnswers  = emptyUserAnswers.set(IsThisYourBusinessPage, pageDetails).success.value
      val application  = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllerRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ProblemDifferentBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(businessName, address, testFullSignOutUrl)(
          request,
          messages(application)
        ).toString
        contentAsString(result)      must not include "GB"
      }
    }

    "must return OK and display country code for a non-GB address, & show correct  view for a GET" in {
      val businessName = "Agent USA Ltd"
      val address      = AddressRegistrationResponse(
        addressLine1 = "2 Wall Street",
        addressLine2 = Some("New York City"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("10500"),
        countryCode = "US"
      )
      val pageDetails  = IsThisYourBusinessPageDetails(
        businessDetails = BusinessDetails(name = businessName, address = address, safeId = testSafeId),
        pageAnswer = Some(true)
      )
      val userAnswers  = emptyUserAnswers.set(IsThisYourBusinessPage, pageDetails).success.value
      val application  = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllerRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ProblemDifferentBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(businessName, address, testFullSignOutUrl)(
          request,
          messages(application)
        ).toString
        contentAsString(result)      must include("US")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(GET, controllerRoute)
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
