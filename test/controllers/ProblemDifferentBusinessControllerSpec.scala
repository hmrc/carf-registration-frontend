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

package controllers

import base.SpecBase
import models.{Address, IsThisYourBusinessPageDetails}
import pages.IsThisYourBusinessPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ProblemDifferentBusinessView

class ProblemDifferentBusinessControllerSpec extends SpecBase {
  val differentBusinessDetailsMessage =
    "You have signed in with a Government Gateway user ID that is linked to different business details."

  "ProblemDifferentBusiness Controller" - {
    "must return OK and not display country code for a GB address, & show correct view for a GET" in {
      val businessName = "Agent ABC Ltd"
      val address      = Address(
        addressLine1 = "2 High Street",
        addressLine2 = Some("Birmingham"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("B23 2AZ"),
        countryCode = "GB"
      )
      val pageDetails  = IsThisYourBusinessPageDetails(
        name = businessName,
        address = address,
        pageAnswer = Some(true)
      )
      val userAnswers  = emptyUserAnswers.set(IsThisYourBusinessPage, pageDetails).success.value
      val application  = applicationBuilder(userAnswers = Some(userAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, routes.ProblemDifferentBusinessController.onPageLoad().url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ProblemDifferentBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(businessName, address)(request, messages(application)).toString
        contentAsString(result)      must not include "GB"
      }
    }

    "must return OK and display country code for a non-GB address, & show correct  view for a GET" in {
      val businessName = "Agent USA Ltd"
      val address      = Address(
        addressLine1 = "2 Wall Street",
        addressLine2 = Some("New York City"),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = Some("10500"),
        countryCode = "USA"
      )
      val pageDetails  = IsThisYourBusinessPageDetails(
        name = businessName,
        address = address,
        pageAnswer = Some(true)
      )
      val userAnswers  = emptyUserAnswers.set(IsThisYourBusinessPage, pageDetails).success.value
      val application  = applicationBuilder(userAnswers = Some(userAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, routes.ProblemDifferentBusinessController.onPageLoad().url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ProblemDifferentBusinessView]
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(businessName, address)(request, messages(application)).toString
        contentAsString(result)      must include("USA")
      }
    }

    "if business name and Address are not found, must return OK, display 'linked to different business details', & show correct view for a GET" in {
      val businessName = ""
      val address      = Address("", None, None, None, None, "")
      val pageDetails  = IsThisYourBusinessPageDetails(
        name = businessName,
        address = address,
        pageAnswer = Some(true)
      )
      val userAnswers  = emptyUserAnswers.set(IsThisYourBusinessPage, pageDetails).success.value
      val application  = applicationBuilder(userAnswers = Some(userAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, routes.ProblemDifferentBusinessController.onPageLoad().url)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ProblemDifferentBusinessView]
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(businessName, address)(request, messages(application)).toString
        contentAsString(result)      must include(differentBusinessDetailsMessage)
      }
    }
  }
}
