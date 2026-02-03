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
import models.responses.{AddressRecord, AddressResponse, CountryRecord}
import models.{Name, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.AddressLookupPage
import pages.individualWithoutId.IndReviewConfirmAddressPage
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.individualWithoutId.IndReviewConfirmAddressView

import scala.concurrent.Future

class IndReviewConfirmAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val indReviewConfirmAddressRoute: String =
    controllers.individualWithoutId.routes.IndReviewConfirmAddressController.onPageLoad(NormalMode).url

  lazy val indReviewConfirmAddressOnSubmitRoute: String =
    controllers.individualWithoutId.routes.IndReviewConfirmAddressController.onSubmit(NormalMode).url

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
          indReviewConfirmAddressRoute
        )

        val result = route(application, request).value

        val view            = application.injector.instanceOf[IndReviewConfirmAddressView]
        val editAddressLink = controllers.routes.PlaceholderController
          .onPageLoad("Must redirect to /register/individual-without-id/address")
          .url

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(address, NormalMode, editAddressLink)(
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
          indReviewConfirmAddressRoute
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
          indReviewConfirmAddressRoute
        )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when user clicks the Continue button" in {

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

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, indReviewConfirmAddressOnSubmitRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        verify(mockSessionRepository, times(1)).set(any())
      }
    }

    "must redirect to Journey Recovery when address is not found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, indReviewConfirmAddressOnSubmitRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when no userAnswers exist" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(GET, indReviewConfirmAddressOnSubmitRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when multiple addresses are found in userAnswers" in {

      val address1 = AddressResponse(
        id = "GB790091234501",
        address = AddressRecord(
          List("1 Test Street", "Line 2"),
          "Testtown",
          "BB00 0BB",
          CountryRecord("GB", "United Kingdom")
        )
      )

      val address2 = AddressResponse(
        id = "GB790091234502",
        address = AddressRecord(
          List("2 Test Street", "Line 3"),
          "Testtown",
          "BB00 0BB",
          CountryRecord("GB", "United Kingdom")
        )
      )

      val userAnswers = emptyUserAnswers.set(AddressLookupPage, Seq(address1, address2)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          indReviewConfirmAddressRoute
        )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must clear (.remove) IndReviewConfirmAddressPage data on page load when an address is found" in {

      val address = AddressResponse(
        id = "GB790091234501",
        address = AddressRecord(
          List("1 Test Street", "Line 2"),
          "Testtown",
          "BB00 0BB",
          CountryRecord("GB", "United Kingdom")
        )
      )

      val userAnswers = emptyUserAnswers
        .set(AddressLookupPage, Seq(address))
        .success
        .value
        .set(IndReviewConfirmAddressPage, address)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          indReviewConfirmAddressRoute
        )

        val result = route(application, request).value

        status(result) mustEqual OK

        val view            = application.injector.instanceOf[IndReviewConfirmAddressView]
        val editAddressLink = controllers.routes.PlaceholderController
          .onPageLoad("Must redirect to /register/individual-without-id/address")
          .url

        contentAsString(result) mustEqual view(address, NormalMode, editAddressLink)(
          request,
          messages(application)
        ).toString
      }
    }

  }
}
