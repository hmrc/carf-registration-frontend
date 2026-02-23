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
import forms.IndWithoutChooseAddressFormProvider
import models.countries.CountryUk
import models.{format, AddressUk, IndFindAddress, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.AddressLookupPage
import pages.individualWithoutId.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.CountryListFactory
import views.html.IndWithoutChooseAddressView

import scala.concurrent.Future

class IndWithoutChooseAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  private val chooseAddressRoute =
    controllers.individualWithoutId.routes.IndWithoutChooseAddressController.onPageLoad(NormalMode).url

  val formProvider       = new IndWithoutChooseAddressFormProvider()
  val form: Form[String] = formProvider()

  val address = AddressUk(
    "1 Test Street",
    Some("Line 2"),
    None,
    "Testtown",
    "BB00 0BB",
    CountryUk("GB", "United Kingdom")
  )

  private lazy val expectedHtml =
    s"We could not find a match for ‘property 1’ — showing all results for B23 1AZ instead."

  "IndWithoutChooseAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(AddressLookupPage, Seq(address))
          .success
          .value
          .set(IndFindAddressPage, IndFindAddress(address.postCode, None))
          .success
          .value
          .set(IndFindAddressAdditionalCallUa, false)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, createAddressRadios(Seq(address)), None)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view with dynamic html element for a GET when additional call is true" in {

      val additionalHtml = generateHtml("property 1", address.postCode)

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(AddressLookupPage, Seq(address))
          .success
          .value
          .set(IndFindAddressPage, IndFindAddress(address.postCode, Some("property 1")))
          .success
          .value
          .set(IndFindAddressAdditionalCallUa, true)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          createAddressRadios(Seq(address)),
          Some(additionalHtml)
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(IndWithoutIdChooseAddressPage, address.format(Seq.empty))
          .success
          .value
          .set(AddressLookupPage, Seq(address))
          .success
          .value
          .set(IndFindAddressPage, IndFindAddress(address.postCode, None))
          .success
          .value
          .set(IndFindAddressAdditionalCallUa, false)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(address.format(Seq.empty)),
          NormalMode,
          createAddressRadios(Seq(address)),
          None
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must return Redirect to journey recovery when IndFindAddressPage or IndFindAddressAdditionalCallUa is missing in ua for GET " in {

      val userAnswers = UserAnswers(userAnswersId).set(AddressLookupPage, Seq(address)).success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController
          .onPageLoad()
          .url
      }
    }

    "must return redirect to journey recovery when AddressLookupPage not present for a GET" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController
          .onPageLoad()
          .url
      }
    }

    "must return redirect to find address page when no address is found but AddressLookup is present for a GET" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(AddressLookupPage, Seq.empty)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.individualWithoutId.routes.IndWithoutIdAddressController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(AddressLookupPage, Seq(address))
          .success
          .value
          .set(IndFindAddressPage, IndFindAddress(address.postCode, None))
          .success
          .value
          .set(IndFindAddressAdditionalCallUa, false)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", address.format(Seq.empty)))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when none of these is submitted and does not store an address" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(AddressLookupPage, Seq(address))
          .success
          .value
          .set(IndFindAddressPage, IndFindAddress(address.postCode, None))
          .success
          .value
          .set(IndFindAddressAdditionalCallUa, false)
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", "none"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(argThat(_.get(IndWithoutIdSelectedChooseAddressPage).isEmpty))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(AddressLookupPage, Seq(address))
          .success
          .value
          .set(IndFindAddressPage, IndFindAddress(address.postCode, None))
          .success
          .value
          .set(IndFindAddressAdditionalCallUa, false)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, createAddressRadios(Seq(address)), None)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when invalid data is submitted when additional call flag is true" in {

      val additionalHtml = generateHtml("property 1", address.postCode)

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(AddressLookupPage, Seq(address))
          .success
          .value
          .set(IndFindAddressPage, IndFindAddress(address.postCode, Some("property 1")))
          .success
          .value
          .set(IndFindAddressAdditionalCallUa, true)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          createAddressRadios(Seq(address)),
          Some(additionalHtml)
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "redirect to Journey Recovery for a POST if no existing user answers data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", "None"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must fail when address selected cannot be found for a POST" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(AddressLookupPage, Seq(address))
          .success
          .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", "Test road 15 not found street"))

        val result = route(application, request).value

        result.failed.futureValue.getMessage mustEqual "Failed to find address"

      }
    }

    def createAddressRadios(addresses: => Seq[AddressUk]): Seq[RadioItem] =
      addresses.map { address =>
        val addressFormatted = address.format(Seq.empty)
        RadioItem(content = Text(s"$addressFormatted"), value = Some(s"$addressFormatted"))
      }

    def generateHtml(property: String, postcode: String) =
      s"""We could not find a match for ‘$property’ — showing all results for $postcode instead."""

  }
}
