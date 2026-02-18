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
import models.responses.{format, AddressRecord, AddressResponse, CountryRecord}
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.AddressLookupPage
import pages.individualWithoutId.{IndWithoutIdAddressPagePrePop, IndWithoutIdChooseAddressPage, IndWithoutIdSelectedChooseAddressPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import views.html.IndWithoutChooseAddressView

import scala.util.Failure
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.*

class IndWithoutChooseAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  private val chooseAddressRoute =
    controllers.individualWithoutId.routes.IndWithoutChooseAddressController.onPageLoad(NormalMode).url

  val formProvider       = new IndWithoutChooseAddressFormProvider()
  val form: Form[String] = formProvider()

  val address = AddressResponse(
    id = "GB790091234501",
    address = AddressRecord(
      List("1 Test Street", "Line 2"),
      "Testtown",
      "BB00 0BB",
      CountryRecord("GB", "United Kingdom")
    )
  )

  "IndWithoutChooseAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers =
        UserAnswers(userAnswersId).set(AddressLookupPage, Seq(address)).success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, createAddressRadios(Seq(address.address)))(
          request,
          messages(application)
        ).toString
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

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        UserAnswers(userAnswersId)
          .set(IndWithoutIdChooseAddressPage, address.address.format)
          .success
          .value
          .set(AddressLookupPage, Seq(address))
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, chooseAddressRoute)

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(address.address.format),
          NormalMode,
          createAddressRadios(Seq(address.address))
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

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
            .withFormUrlEncodedBody(("value", address.address.format))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when none of these is submitted and not store an address" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

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

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, chooseAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[IndWithoutChooseAddressView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, createAddressRadios(Seq(address.address)))(
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

    def createAddressRadios(addressRecords: => Seq[AddressRecord]): Seq[RadioItem] =
      addressRecords.map { addressRecord =>
        val addressFormatted = addressRecord.format
        RadioItem(content = Text(s"$addressFormatted"), value = Some(s"$addressFormatted"))
      }
  }
}
