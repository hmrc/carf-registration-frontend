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
import generators.Generators
import models.JourneyType.IndWithoutId
import models.error.ApiError
import models.requests.SearchByPostcodeRequest
import models.responses.{AddressRecord, AddressResponse, CountryRecord}
import models.{IndFindAddress, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.individualWithoutId.IndFindAddressPage
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.AddressLookupService
import views.html.individualWithoutId.IndFindAddressView

import scala.concurrent.Future

class IndFindAddressControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach with Generators {

  val formProvider: IndFindAddressFormProvider       = new IndFindAddressFormProvider()
  val form: Form[IndFindAddress]                     = formProvider()
  val mockAddressLookupService: AddressLookupService = mock[AddressLookupService]

  lazy val indFindAddressRoute: String =
    controllers.individualWithoutId.routes.IndFindAddressController.onPageLoad(NormalMode).url

  override def beforeEach(): Unit = {
    reset(mockAddressLookupService)
    reset(mockSessionRepository)
    super.beforeEach()
  }

  val searchByPostcodeValidResponse: Seq[AddressResponse] = Seq(
    AddressResponse(
      id = "Test-Id",
      address = AddressRecord(
        lines = List("Address-Line1", "Address-Line2"),
        town = "Bristol",
        postcode = validPostcodes.sample.value,
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    )
  )

  val oneAddress: Seq[AddressResponse] = Seq(
    AddressResponse(
      id = "123",
      address = AddressRecord(
        lines = List("1 test", "1 Test Street", "Testington"),
        town = " Test Town",
        postcode = validPostcodes.sample.value,
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    )
  )

  val addresses: Seq[AddressResponse] = Seq(
    AddressResponse(
      id = "123",
      address = AddressRecord(
        lines = List("1 test", "1 Test Street", "Testington"),
        town = "South Test Town",
        postcode = validPostcodes.sample.value,
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    ),
    AddressResponse(
      id = "124",
      address = AddressRecord(
        lines = List("2 test", "2 Test Street", "Testington"),
        town = "East Test Town",
        postcode = validPostcodes.sample.value,
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    ),
    AddressResponse(
      id = "125",
      address = AddressRecord(
        lines = List("1 test", "2 Test Street", "Testington"),
        town = "North Townshire",
        postcode = validPostcodes.sample.value,
        country = CountryRecord(code = "UK", name = "United Kingdom")
      )
    )
  )

  val userAnswers = UserAnswers(
    id = userAnswersId,
    journeyType = Some(IndWithoutId),
    data = Json.obj(
      IndFindAddressPage.toString -> Json.obj(
        "postcode"             -> "AA1 1AA",
        "propertyNameOrNumber" -> "value 2"
      )
    )
  )

  "IndFindAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, indFindAddressRoute)

        val view = application.injector.instanceOf[IndFindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, indFindAddressRoute)

        val view = application.injector.instanceOf[IndFindAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(IndFindAddress("AA1 1AA", Some("value 2"))), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when postcode has returned one address" in {

      val onwardRouteOneAddress = routes.PlaceholderController.onPageLoad(
        s"Must redirect to /register/individual-without-id/review-address (CARF-173)"
      )

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(Some("value 2")))(any(), any()))
        .thenReturn(Future.successful(Right(oneAddress)))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
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
      }
    }

    "must redirect to the next page when postcode has returned more than one addresses" in {

      val onwardRouteMultipleAddresses = routes.PlaceholderController.onPageLoad(
        s"Must redirect to /register/individual-without-id/choose-address (CARF-312)"
      )

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockAddressLookupService.postcodeSearch(eqTo("TE1 1ST"), eqTo(None))(any(), any()))
        .thenReturn(Future.successful(Right(addresses)))

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
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
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
        .thenReturn(Future.successful(Right(Nil)))

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

        contentAsString(result) mustEqual view(formWithError, NormalMode)(request, messages(application)).toString

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
