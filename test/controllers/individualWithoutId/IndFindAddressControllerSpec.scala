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

package controllers.individualWithoutId

import base.SpecBase
import controllers.routes
import forms.individualWithoutId.IndFindAddressFormProvider
import models.error.ApiError
import models.{AddressAndUPRN, AddressUk, ChangeMode, IndFindAddress, NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, argThat, eq as eqTo}
import org.mockito.Mockito.*
import pages.individualWithoutId.*
import play.api.data.Form
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.AddressLookupService
import views.html.individualWithoutId.IndFindAddressView

import scala.concurrent.Future

class IndFindAddressControllerSpec extends SpecBase {

  val formProvider: IndFindAddressFormProvider       = new IndFindAddressFormProvider()
  val form: Form[IndFindAddress]                     = formProvider()
  val mockAddressLookupService: AddressLookupService = mock[AddressLookupService]

  lazy val indFindAddressRoute: String =
    controllers.individualWithoutId.routes.IndFindAddressController.onPageLoad(NormalMode).url

  override def beforeEach(): Unit = {
    reset(mockAddressLookupService)
    super.beforeEach()
  }

  private def expectedManualUrl: String =
    controllers.individualWithoutId.routes.IndWithoutIdAddressController.onPageLoad(NormalMode).url

  "IndFindAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        val request = FakeRequest(GET, indFindAddressRoute)

        val view = application.injector.instanceOf[IndFindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, expectedManualUrl)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and remove IndWithoutIdAddressPagePrePop from ua when performing a GET" in {
      val userAnswers = emptyUserAnswers.set(IndWithoutIdAddressPagePrePop, testAddressUk).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        val request = FakeRequest(GET, indFindAddressRoute)

        val view = application.injector.instanceOf[IndFindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, expectedManualUrl)(
          request,
          messages(application)
        ).toString
        verify(mockSessionRepository).set(argThat(_.get(IndWithoutIdAddressPagePrePop).isEmpty))
      }
    }

    "must return OK and NOT remove IndWithoutIdAddressPagePrePop from ua when performing a GET via Change mode" in {
      val userAnswers = emptyUserAnswers.withPage(IndWithoutIdAddressPagePrePop, testAddressUk)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        val indFindAddressRouteChangeMode: String =
          controllers.individualWithoutId.routes.IndFindAddressController.onPageLoad(ChangeMode).url

        val expectedManualUrlChangeMode: String =
          controllers.individualWithoutId.routes.IndWithoutIdAddressController.onPageLoad(ChangeMode).url

        val request = FakeRequest(GET, indFindAddressRouteChangeMode)

        val view = application.injector.instanceOf[IndFindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, ChangeMode, expectedManualUrlChangeMode)(
          request,
          messages(application)
        ).toString
        verify(mockSessionRepository).set(argThat(_.get(IndWithoutIdAddressPagePrePop).isDefined))
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = emptyUserAnswers
        .withPage(IndFindAddressPage, IndFindAddress(postcode = "AA1 1AA", propertyNameOrNumber = Some("value 2")))

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      running(application) {
        val request = FakeRequest(GET, indFindAddressRoute)

        val view = application.injector.instanceOf[IndFindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(IndFindAddress("AA1 1AA", Some("value 2"))),
          NormalMode,
          expectedManualUrl
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page and clear IndFindAddressAdditionalCallUa and AddressLookupPage on submit when postcode has returned one address" in {
      val userAnswers = emptyUserAnswers
        .withPage(IndFindAddressAdditionalCallUa, true)
        .withPage(AddressLookupPage, testAddressAndUprns)

      val onwardRouteOneAddress =
        controllers.individualWithoutId.routes.IndReviewConfirmAddressController.onPageLoad(NormalMode)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(Some("value 2")))(any(), any()))
        .thenReturn(
          Future.successful(
            Right(Seq(AddressAndUPRN(testAddressUk, testUPRN)), false)
          )
        )

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, indFindAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"), ("propertyNameOrNumber", "value 2"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRouteOneAddress.url
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(Some("value 2")))(any(), any())
        verify(mockSessionRepository, times(1)).set(
          argThat(ua =>
            ua.get(AddressUPRNUserAnswers).contains(testUPRN) &&
              ua.get(IndWithoutIdAddressPagePrePop).contains(testAddressUk) &&
              ua.get(IndFindAddressAdditionalCallUa).isEmpty &&
              ua.get(AddressLookupPage).isEmpty
          )
        )
      }
    }

    "must redirect to the next page on submit when postcode has returned more than one address" in {

      val onwardRouteMultipleAddresses =
        controllers.individualWithoutId.routes.IndWithoutIdChooseAddressController.onPageLoad(NormalMode)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(Future.successful(Right(testAddressAndUprns, false)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, indFindAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRouteMultipleAddresses.url
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
      }
    }

    "must redirect to the next page on submit when postcode has returned more than one address and retry has happened" in {

      val onwardRouteMultipleAddresses =
        controllers.individualWithoutId.routes.IndWithoutIdChooseAddressController.onPageLoad(NormalMode)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(Future.successful(Right(testAddressAndUprns, true)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, indFindAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRouteMultipleAddresses.url
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
        verify(mockSessionRepository, times(1)).set(any())
        verify(mockSessionRepository).set(argThat(_.get(IndFindAddressAdditionalCallUa).isDefined))
      }
    }

    "must clear UPRN and IndWithoutIdAddressPagePrePop from user answers on submit when multiple addresses are returned" in {
      val userAnswersWithUprn = emptyUserAnswers
        .withPage(AddressUPRNUserAnswers, testUPRN)
        .withPage(IndWithoutIdAddressPagePrePop, testAddressUk)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(Future.successful(Right(testAddressAndUprns, true)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithUprn))
          .overrides(
            bind[AddressLookupService].toInstance(mockAddressLookupService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, indFindAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
        verify(mockSessionRepository, times(1)).set(any())
        verify(mockSessionRepository).set(
          argThat(ua =>
            ua.get(AddressLookupPage).contains(testAddressAndUprns) &&
              ua.get(AddressUPRNUserAnswers).isEmpty &&
              ua.get(IndWithoutIdAddressPagePrePop).isEmpty
          )
        )
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, indFindAddressRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[IndFindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, expectedManualUrl)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, indFindAddressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, indFindAddressRoute)
            .withFormUrlEncodedBody(("postcode", "value 1"), ("propertyNameOrNumber", "value 2"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return Bad Request with error when postcode search returns no addresses" in {

      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(
          Future.successful(
            Right((Nil, false))
          )
        )

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AddressLookupService].toInstance(mockAddressLookupService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, indFindAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val view = application.injector.instanceOf[IndFindAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST

        val boundForm     = form.bind(Map("postcode" -> "TE1 1ST"))
        val formWithError = boundForm.withError("postcode", "indFindAddress.error.postcode.notFound")

        contentAsString(result) mustEqual view(formWithError, NormalMode, expectedManualUrl)(
          request,
          messages(application)
        ).toString

        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
      }
    }

    "must redirect to Journey Recovery when address lookup service returns an error" in {

      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(Future.successful(Left(ApiError.BadRequestError)))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[AddressLookupService].toInstance(mockAddressLookupService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, indFindAddressRoute)
            .withFormUrlEncodedBody(("postcode", "TE1 1ST"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        verify(mockAddressLookupService, times(1)).postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any())
      }
    }

  }
}
