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
import forms.YourUniqueTaxpayerReferenceFormProvider
import models.OrganisationRegistrationType.*
import models.{NormalMode, OrganisationRegistrationType, UniqueTaxpayerReference, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{OrganisationRegistrationTypePage, YourUniqueTaxpayerReferencePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.YourUniqueTaxpayerReferenceView

import scala.concurrent.Future

class YourUniqueTaxpayerReferenceControllerSpec extends SpecBase with MockitoSugar {

  private def getTaxTypeMessage(userAnswers: UserAnswers) =
    userAnswers.get(OrganisationRegistrationTypePage) match {
      case Some(LimitedCompany) | Some(Trust) => "yourUniqueTaxpayerReference.ltdUnincorporated"
      case Some(Partnership) | Some(LLP)      => "yourUniqueTaxpayerReference.partnershipLlp"
      case _                                  => "yourUniqueTaxpayerReference.soleTraderIndividual"
    }

  def onwardRoute = Call("GET", "/foo")

  lazy val yourUniqueTaxpayerReferenceRoute: String =
    routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode).url

  "YourUniqueTaxpayerReference Controller" - {

    "must return OK and the correct view for a GET when option is a LimitedCompany" in {
      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, LimitedCompany)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))

      running(application) {
        val request = FakeRequest(GET, yourUniqueTaxpayerReferenceRoute)

        val result            = route(application, request).value
        val form              = new YourUniqueTaxpayerReferenceFormProvider().apply(
          taxType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )
        val view              = application.injector.instanceOf[YourUniqueTaxpayerReferenceView]
        val taxTypeMessageKey = getTaxTypeMessage(userAnswers)

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode, taxTypeMessageKey)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when option is a Partnership" in {
      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, Partnership)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))

      running(application) {
        val request = FakeRequest(GET, yourUniqueTaxpayerReferenceRoute)

        val result            = route(application, request).value
        val form              = new YourUniqueTaxpayerReferenceFormProvider().apply(
          taxType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )
        val view              = application.injector.instanceOf[YourUniqueTaxpayerReferenceView]
        val taxTypeMessageKey = getTaxTypeMessage(userAnswers)

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode, taxTypeMessageKey)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET when option is a SoleTrader or individual" in {
      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, SoleTrader)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))

      running(application) {
        val request = FakeRequest(GET, yourUniqueTaxpayerReferenceRoute)

        val result            = route(application, request).value
        val form              = new YourUniqueTaxpayerReferenceFormProvider().apply(
          taxType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )
        val view              = application.injector.instanceOf[YourUniqueTaxpayerReferenceView]
        val taxTypeMessageKey = getTaxTypeMessage(userAnswers)

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode, taxTypeMessageKey)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val utr         = UniqueTaxpayerReference("exampleUtr")
      val userAnswers = UserAnswers(userAnswersId)
        .set(OrganisationRegistrationTypePage, LimitedCompany)
        .success
        .value
        .set(YourUniqueTaxpayerReferencePage, utr)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, yourUniqueTaxpayerReferenceRoute)

        val view              = application.injector.instanceOf[YourUniqueTaxpayerReferenceView]
        val form              = new YourUniqueTaxpayerReferenceFormProvider().apply(
          taxType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )
        val taxTypeMessageKey = getTaxTypeMessage(userAnswers)

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(utr), NormalMode, taxTypeMessageKey)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, SoleTrader)
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      val utrValue                     = "1234567890"
      val utr: UniqueTaxpayerReference = UniqueTaxpayerReference(utrValue)

      running(application) {
        val request =
          FakeRequest(POST, yourUniqueTaxpayerReferenceRoute)
            .withFormUrlEncodedBody(("value", utr.uniqueTaxPayerReference))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, LimitedCompany)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, yourUniqueTaxpayerReferenceRoute)
            .withFormUrlEncodedBody(("value", ""))

        val form = new YourUniqueTaxpayerReferenceFormProvider().apply(
          taxType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[YourUniqueTaxpayerReferenceView]

        val taxTypeMessageKey = getTaxTypeMessage(userAnswers)

        val result = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, taxTypeMessageKey)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, yourUniqueTaxpayerReferenceRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, yourUniqueTaxpayerReferenceRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
