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
import forms.individualWithoutId.IndWithoutIdAddressFormProvider
import generators.Generators
import models.countries.{Country, UnitedKingdom}
import models.{AddressUK, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.individualWithoutId.IndWithoutIdAddressPagePrePop
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.CountryListFactory
import views.html.AddressView

import scala.concurrent.Future

class IndWithoutIdAddressControllerSpec
    extends SpecBase
    with MockitoSugar
    with ScalaCheckPropertyChecks
    with Generators {

  def onwardRoute = Call("GET", "/foo")

  private val uk: Country                 = Country("UK", "United Kingdom")
  private val france: Country             = Country("FR", "France")
  private val jersey: Country             = Country("JE", "Jersey")
  private val mockCountries: Set[Country] = Set(uk, france, jersey)

  private val formProvider = new IndWithoutIdAddressFormProvider()
  private val form         = formProvider()

  private lazy val addressRoute =
    controllers.individualWithoutId.routes.IndWithoutIdAddressController.onPageLoad(NormalMode).url

  inline final val addressRegex     = """^[a-zA-Z0-9 \.&`\-\'\^]*$"""
  inline final val addressMaxLength = 35

  private val address = AddressUK("123 Test Street", None, "Birmingham", None, "B23 2AZ", "UK")

  val mockCountryListFactory: CountryListFactory = mock[CountryListFactory]

  override def beforeEach(): Unit = {
    reset(mockCountryListFactory, mockSessionRepository)
    super.beforeEach()
  }

  "IndWithoutIdAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockCountryListFactory.countrySelectList(any(), any())).thenReturn(Seq.empty)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request = FakeRequest(GET, addressRoute)
        val view    = application.injector.instanceOf[AddressView]
        val result  = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, Seq.empty)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered via pre-pop" in {

      val postCode  = validPostcodes.sample.value
      val addressUK = AddressUK("addressLine1", Some("addressLine2"), "town", None, postCode, UnitedKingdom.code)

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(IndWithoutIdAddressPagePrePop, addressUK)
          .success
          .value

      when(mockCountryListFactory.countrySelectList(any(), any())).thenReturn(Seq.empty)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request = FakeRequest(GET, addressRoute)

        val view = application.injector.instanceOf[AddressView]

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form.fill(addressUK), NormalMode, Seq.empty)(
          request,
          messages(application)
        ).toString
      }
    }
  }

  "must redirect to the next page when valid data is submitted" in {
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

    val application =
      applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
        )
        .build()

    running(application) {
      val request =
        FakeRequest(POST, addressRoute)
          .withFormUrlEncodedBody(
            "addressLine1" -> "value 1",
            "addressLine2" -> "value 2",
            "townOrCity"   -> address.townOrCity,
            "postcode"     -> address.postCode,
            "country"      -> UnitedKingdom.code
          )

      val result = route(application, request).value

      status(result)                 mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual onwardRoute.url
    }
  }

  "must return a Bad Request and errors when invalid data is submitted" in {

    when(mockCountryListFactory.countrySelectList(any(), any())).thenReturn(Seq.empty)

    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
      .build()

    running(application) {
      val request =
        FakeRequest(POST, addressRoute)
          .withFormUrlEncodedBody(("addressLine1", ""))

      val boundForm = form.bind(Map("addressLine1" -> ""))
      val view      = application.injector.instanceOf[AddressView]
      val result    = route(application, request).value

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
      val request = FakeRequest(GET, addressRoute)

      val result = route(application, request).value

      status(result)                 mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
    }
  }

  "must redirect to Journey Recovery for a POST if no existing data is found" in {

    val application = applicationBuilder(userAnswers = None).build()

    running(application) {
      val request =
        FakeRequest(POST, addressRoute)
          .withFormUrlEncodedBody(("addressLine1", "value 1"), ("addressLine2", "value 2"))

      val result = route(application, request).value

      status(result)                 mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
    }
  }
}
