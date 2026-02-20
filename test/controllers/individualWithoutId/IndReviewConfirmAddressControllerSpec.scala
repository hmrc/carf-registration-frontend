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
import pages.individualWithoutId.{IndReviewConfirmAddressPageForNavigatorOnly, IndWithoutIdUkAddressInUserAnswers}
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
      val userAnswers = emptyUserAnswers.set(AddressLookupPage, Seq(testAddressUk)).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(
          GET,
          indReviewConfirmAddressRoute
        )

        val result = route(application, request).value

        val view            = application.injector.instanceOf[IndReviewConfirmAddressView]
        val editAddressLink =
          controllers.individualWithoutId.routes.IndWithoutIdAddressController.onPageLoad(NormalMode).url

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testAddressUk, NormalMode, editAddressLink)(
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
      val userAnswers = emptyUserAnswers.set(AddressLookupPage, Seq(testAddressUk)).success.value

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
      val userAnswers = emptyUserAnswers.set(AddressLookupPage, Seq(testAddressUk, testAddressUk)).success.value

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
  }
}
