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
import models.{Address, BusinessDetails, IsThisYourBusinessPageDetails, NormalMode, UniqueTaxpayerReference, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.InternalServerException
import pages.{IndexPage, IsThisYourBusinessPage, WhatIsTheNameOfYourBusinessPage, YourUniqueTaxpayerReferencePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.RegistrationService
import views.html.IsThisYourBusinessView
import uk.gov.hmrc.http.HeaderCarrier
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

import scala.concurrent.Future

class IsThisYourBusinessControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call                            = Call("GET", "/foo")
  val formProvider: IsThisYourBusinessFormProvider = new IsThisYourBusinessFormProvider()
  val form: Form[Boolean]                          = formProvider()
  val mockRegistrationService: RegistrationService = mock[RegistrationService]

  val businessUtrString: String             = "1234567890"
  val businessTestBusiness: BusinessDetails = BusinessDetails(
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

  val testPageDetails: IsThisYourBusinessPageDetails = IsThisYourBusinessPageDetails(
    name = businessTestBusiness.name,
    address = businessTestBusiness.address,
    pageAnswer = None
  )

  lazy val isThisYourBusinessControllerRoute: String = routes.IsThisYourBusinessController.onPageLoad(NormalMode).url
  lazy val postRoute: String                         = routes.IsThisYourBusinessController.onSubmit(NormalMode).url

  override def beforeEach(): Unit = {
    super.beforeEach()
    org.mockito.Mockito.reset(mockRegistrationService, mockSessionRepository)
    when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
  }

  "IsThisYourBusinessController" - {

    "on an auto-match journey" - {
      "must return OK and the correct view when a UTR is found via IndexPage" in {
        val userAnswers = UserAnswers(userAnswersId).set(IndexPage, testUtr).success.value
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
        val userAnswers = UserAnswers(userAnswersId).set(IndexPage, testUtr).success.value
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
    }

    "on a manual-entry journey" - {
      "must return OK and the correct view when UTR and Name are provided" in {
        val userAnswers = UserAnswers(userAnswersId)
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
      "must redirect to Business not Identified when business is not found by UTR navigating through the Journey" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(YourUniqueTaxpayerReferencePage, testUtr)
          .success
          .value
          .set(WhatIsTheNameOfYourBusinessPage, "some name")
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
          redirectLocation(result).value mustEqual routes.PlaceholderController
            .onPageLoad("Must redirect to /problem/business-not-identified (CARF-147)")
            .url
        }
      }
    }

    "must redirect to Journey Recovery when no UTR is found in either journey" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, isThisYourBusinessControllerRoute)
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
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
          verify(mockSessionRepository).set(any[UserAnswers])
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
