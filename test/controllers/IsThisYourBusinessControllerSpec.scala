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
import models.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import pages.organisation.{RegistrationTypePage, WhatIsTheNameOfYourBusinessPage, YourUniqueTaxpayerReferencePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.RegistrationService
import views.html.IsThisYourBusinessView

import scala.concurrent.Future

class IsThisYourBusinessControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {

  def onwardRoute: Call                            = Call("GET", "/foo")
  val formProvider: IsThisYourBusinessFormProvider = new IsThisYourBusinessFormProvider()
  val form: Form[Boolean]                          = formProvider()
  val mockRegistrationService: RegistrationService = mock

  val businessTestBusiness: BusinessDetails = BusinessDetails(
    name = "Test Business Ltd",
    address = Address("123 Test Street", Some("Birmingham"), None, None, Some("B23 2AZ"), "GB")
  )

  val soleTraderTestBusiness: BusinessDetails = BusinessDetails(
    name = "Test Name Sole Trader",
    address = Address("1 Test Street", Some("Testville"), None, None, Some("T3 5ST"), "GB")
  )

  val testPageDetails: IsThisYourBusinessPageDetails = IsThisYourBusinessPageDetails(
    name = businessTestBusiness.name,
    address = businessTestBusiness.address,
    pageAnswer = None
  )

  lazy val isThisYourBusinessControllerRoute: String = routes.IsThisYourBusinessController.onPageLoad(NormalMode).url
  lazy val postRoute: String                         = routes.IsThisYourBusinessController.onSubmit(NormalMode).url

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRegistrationService, mockSessionRepository)
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
  }

  "IsThisYourBusinessController" - {

    "on a Sole Trader journey" - {
      "must return OK and the correct view for a successful match" in {
        val soleTraderUtr = UniqueTaxpayerReference("1234567890")
        val userAnswers   = UserAnswers(userAnswersId)
          .set(RegistrationTypePage, RegistrationType.SoleTrader)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, soleTraderUtr)
          .success
          .value

        when(mockRegistrationService.getBusinessWithUserInput(eqTo(userAnswers))(any()))
          .thenReturn(Future.successful(Some(soleTraderTestBusiness)))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
          val result  = route(application, request).value

          status(result)     mustEqual OK
          contentAsString(result) must include("Test Name Sole Trader")

          verify(mockRegistrationService).getBusinessWithUserInput(eqTo(userAnswers))(any())

        }
      }

      "must redirect to Sole Trader Not Identified page for an unsuccessful match" in {
        val soleTraderUtr = UniqueTaxpayerReference("3000000000")
        val userAnswers   = UserAnswers(userAnswersId)
          .set(RegistrationTypePage, RegistrationType.SoleTrader)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, soleTraderUtr)
          .success
          .value

        when(mockRegistrationService.getBusinessWithUserInput(eqTo(userAnswers))(any()))
          .thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PlaceholderController
            .onPageLoad("Must redirect to /problem/sole-trader-not-identified (CARF-129)")
            .url
        }
      }
    }

    "on an Organisation auto-match journey" - {
      "must return OK and the correct view when a UTR is found via IndexPage" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(RegistrationTypePage, RegistrationType.LimitedCompany)
          .success
          .value
          .set(IndexPage, testUtr)
          .success
          .value

        when(mockRegistrationService.getBusinessWithEnrolmentCtUtr(eqTo(testUtr.uniqueTaxPayerReference))(any()))
          .thenReturn(Future.successful(Some(businessTestBusiness)))
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

      "must redirect to Journey Recovery when the service finds no business" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(IndexPage, testUtr)
          .success
          .value
          .set(RegistrationTypePage, RegistrationType.LLP)
          .success
          .value

        when(mockRegistrationService.getBusinessWithEnrolmentCtUtr(any())(any()))
          .thenReturn(Future.successful(None))

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

      "must return an Internal Server Error when the registration service fails" in {
        val userAnswers = UserAnswers(userAnswersId).set(IndexPage, testUtr).success.value

        when(mockRegistrationService.getBusinessWithEnrolmentCtUtr(any())(any()))
          .thenReturn(Future.failed(new Exception("bang")))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
          val result  = route(application, request).value.failed.futureValue
          result mustBe a[Exception]
        }
      }
    }

    "on an Organisation manual-entry journey" - {
      "must return OK and the correct view when UTR and Business name are provided" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(RegistrationTypePage, RegistrationType.LimitedCompany)
          .success
          .value
          .set(YourUniqueTaxpayerReferencePage, testUtr)
          .success
          .value
          .set(WhatIsTheNameOfYourBusinessPage, "some name")
          .success
          .value

        when(mockRegistrationService.getBusinessWithUserInput(eqTo(userAnswers))(any()))
          .thenReturn(Future.successful(Some(businessTestBusiness)))
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

      "must redirect to Business Not Identified when no business is found" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(YourUniqueTaxpayerReferencePage, testUtr)
          .success
          .value
          .set(WhatIsTheNameOfYourBusinessPage, "some name")
          .success
          .value
          .set(RegistrationTypePage, RegistrationType.LimitedCompany)
          .success
          .value

        when(mockRegistrationService.getBusinessWithUserInput(any())(any()))
          .thenReturn(Future.successful(None))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[RegistrationService].toInstance(mockRegistrationService))
          .build()

        running(application) {
          val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.organisation.routes.BusinessNotIdentifiedController
            .onPageLoad()
            .url
        }
      }
    }

    "for all journeys" - {
      "must redirect to Journey Recovery for a GET if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
          val result  = route(application, request).value
          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(POST, postRoute).withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value
          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {
      "must redirect to the next page when valid data is submitted" in {
        val userAnswers = UserAnswers(userAnswersId).set(IsThisYourBusinessPage, testPageDetails).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

        running(application) {
          val request = FakeRequest(POST, postRoute).withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value
          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockSessionRepository).set(any)
        }
      }

      "must return a Bad Request when invalid data is submitted" in {
        val userAnswers = UserAnswers(userAnswersId).set(IsThisYourBusinessPage, testPageDetails).success.value
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, postRoute).withFormUrlEncodedBody(("value", ""))
          val result  = route(application, request).value
          status(result) mustEqual BAD_REQUEST
        }
      }

      "must redirect to Journey Recovery when no business details found in UserAnswers on POST" in {
        val userAnswers = UserAnswers(userAnswersId)
        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(POST, postRoute).withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
