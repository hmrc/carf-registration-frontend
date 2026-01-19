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

package controllers.individualWithoutId

import base.SpecBase
import models.NormalMode
import models.responses.{AddressRecord, AddressResponse, CountryRecord}
import pages.AddressLookupPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.individualWithoutId.IndReviewConfirmAddressView

class IndReviewConfirmAddressControllerSpec extends SpecBase {

  "IndReviewConfirmAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val address = AddressResponse(
        id = "GB790091234501",
        address = AddressRecord(
          List("1 Test Street", "Line 2"),
          "Testtown",
          "BB00 0BB",
          CountryRecord("GB", "United Kingdom")
        )
      )

      val userAnswers = emptyUserAnswers.set(AddressLookupPage, Seq(address)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.individualWithoutId.routes.IndReviewConfirmAddressController.onPageLoad().url
        )

        val result = route(application, request).value

        val view            = application.injector.instanceOf[IndReviewConfirmAddressView]
        val editAddressLink = controllers.routes.PlaceholderController
          .onPageLoad("Must redirect to /register/individual-without-id/address")
          .url
        val nextPageLink    = controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode).url

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(address, NormalMode, editAddressLink, nextPageLink)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery when an address is not found in userAnswers" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.individualWithoutId.routes.IndReviewConfirmAddressController.onPageLoad().url
        )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when no userAnswers exist" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.individualWithoutId.routes.IndReviewConfirmAddressController.onPageLoad().url
        )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
