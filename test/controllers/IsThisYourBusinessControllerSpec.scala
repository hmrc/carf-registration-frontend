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
import forms.IsThisYourBusinessFormProvider
import models.{Address, Business, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{IndexPage, IsThisYourBusinessPage, YourUniqueTaxpayerReferencePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.RegistrationService
import views.html.IsThisYourBusinessView

import scala.concurrent.Future

class IsThisYourBusinessControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute             = Call("GET", "/foo")
  val formProvider            = new IsThisYourBusinessFormProvider()
  val form                    = formProvider()
  val mockRegistrationService = mock[RegistrationService]

  val businessUtrString    = "1234567890"
  val businessTestBusiness = Business(
    name = "Test Business Ltd",
    address = Address(
      addressLine1 = "123 Test Street",
      addressLine2 = Some("Birmingham"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("B23 2AZ"),
      countryCode = "GB"
    )
  )

  lazy val isThisYourBusinessControllerRoute = routes.IsThisYourBusinessController.onPageLoad(NormalMode).url

  "IsThisYourBusinessController" - {

    "must return OK and the correct view for a GET with UTR from YourUniqueTaxpayerReferencePage" in {
      when(mockRegistrationService.getBusinessByUtr(businessUtrString)) thenReturn Future.successful(
        Some(businessTestBusiness)
      )

      val userAnswers = UserAnswers(userAnswersId)
        .set(YourUniqueTaxpayerReferencePage, testUtr)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[IsThisYourBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, businessTestBusiness)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK and the correct view for a GET with UTR from IndexPage (auto-matched)" in {
      when(mockRegistrationService.getBusinessByUtr(businessUtrString)) thenReturn Future.successful(
        Some(businessTestBusiness)
      )

      val userAnswers = UserAnswers(userAnswersId)
        .set(IndexPage, testUtr)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[IsThisYourBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, businessTestBusiness)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly when the question has previously been answered" in {
      when(mockRegistrationService.getBusinessByUtr(businessUtrString)) thenReturn Future.successful(
        Some(businessTestBusiness)
      )

      val userAnswers = UserAnswers(userAnswersId)
        .set(YourUniqueTaxpayerReferencePage, testUtr)
        .success
        .value
        .set(IsThisYourBusinessPage, true)  // Previously answered "Yes"
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[IsThisYourBusinessView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, businessTestBusiness)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery when no UTR is found on GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when business is not found by UTR on GET" in {
      when(mockRegistrationService.getBusinessByUtr(businessUtrString)) thenReturn Future.successful(None)

      val userAnswers = UserAnswers(userAnswersId)
        .set(YourUniqueTaxpayerReferencePage, testUtr)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockRegistrationService.getBusinessByUtr(businessUtrString)) thenReturn Future.successful(
        Some(businessTestBusiness)
      )

      val userAnswers = UserAnswers(userAnswersId)
        .set(YourUniqueTaxpayerReferencePage, testUtr)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[RegistrationService].toInstance(mockRegistrationService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, isThisYourBusinessControllerRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request when invalid data is submitted" in {
      when(mockRegistrationService.getBusinessByUtr(businessUtrString)) thenReturn Future.successful(
        Some(businessTestBusiness)
      )

      val userAnswers = UserAnswers(userAnswersId)
        .set(YourUniqueTaxpayerReferencePage, testUtr)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(POST, isThisYourBusinessControllerRoute)
          .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery when no UTR is found on POST" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(POST, isThisYourBusinessControllerRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when business is not found by UTR on POST (no cached business)" in {
      when(mockRegistrationService.getBusinessByUtr(businessUtrString)) thenReturn Future.successful(None)

      val userAnswers = UserAnswers(userAnswersId)
        .set(YourUniqueTaxpayerReferencePage, testUtr)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
        .build()

      running(application) {
        val request = FakeRequest(POST, isThisYourBusinessControllerRoute)
          .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
