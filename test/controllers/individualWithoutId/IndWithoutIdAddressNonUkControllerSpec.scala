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
import controllers.routes
import forms.individualWithoutId.IndWithoutIdAddressNonUkFormProvider
import models.{Country, IndWithoutIdAddressNonUk, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.individualWithoutId.IndWithoutIdAddressNonUkPage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.CountryListFactory
import views.html.individualWithoutId.IndWithoutIdAddressNonUkView

import scala.concurrent.Future

class IndWithoutIdAddressNonUkControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val mockCountryListFactory = mock[CountryListFactory]
  val countries              = Seq(Country("FR", "France"), Country("DE", "Germany"))

  when(mockCountryListFactory.countryList).thenReturn(
    Some(
      Seq(
        Country("GB", "United Kingdom"),
        Country("FR", "France"),
        Country("DE", "Germany")
      )
    )
  )

  when(mockCountryListFactory.countrySelectList(any(), any())).thenReturn(Seq.empty)

  val formProvider = new IndWithoutIdAddressNonUkFormProvider()
  val form         = formProvider(countries)

  lazy val indWithoutIdAddressNonUkRoute =
    controllers.individualWithoutId.routes.IndWithoutIdAddressNonUkController.onPageLoad(NormalMode).url

  val validAddress = IndWithoutIdAddressNonUk(
    "123 Main Street",
    Some("Apt 4"),
    "Paris",
    Some("Ile-de-France"),
    Some("75001"),
    Country("FR", "France")
  )

  val userAnswers = UserAnswers(userAnswersId)
    .set(IndWithoutIdAddressNonUkPage, validAddress)
    .success
    .value

  "IndWithoutIdAddressNonUk Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request = FakeRequest(GET, indWithoutIdAddressNonUkRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IndWithoutIdAddressNonUkView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, Seq.empty)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request = FakeRequest(GET, indWithoutIdAddressNonUkRoute)

        val view = application.injector.instanceOf[IndWithoutIdAddressNonUkView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAddress), NormalMode, Seq.empty)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[CountryListFactory].toInstance(mockCountryListFactory)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, indWithoutIdAddressNonUkRoute)
            .withFormUrlEncodedBody(
              ("addressLine1", "123 Main Street"),
              ("addressLine2", "Apt 4"),
              ("townOrCity", "Paris"),
              ("region", "Ile-de-France"),
              ("postcode", "75001"),
              ("country", "FR")
            )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, indWithoutIdAddressNonUkRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[IndWithoutIdAddressNonUkView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, Seq.empty)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, indWithoutIdAddressNonUkRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, indWithoutIdAddressNonUkRoute)
            .withFormUrlEncodedBody(
              ("addressLine1", "123 Main Street"),
              ("townOrCity", "Paris"),
              ("country", "FR")
            )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when countries list cannot be retrieved" in {
      val mockCountryListFactoryEmpty = mock[CountryListFactory]
      when(mockCountryListFactoryEmpty.countryList).thenReturn(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactoryEmpty))
        .build()

      running(application) {
        val request = FakeRequest(GET, indWithoutIdAddressNonUkRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
