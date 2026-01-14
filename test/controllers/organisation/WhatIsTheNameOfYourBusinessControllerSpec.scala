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

package controllers.organisation

import base.SpecBase
import controllers.routes
import forms.organisation.WhatIsTheNameOfYourBusinessFormProvider
import models.OrganisationRegistrationType.*
import models.{Address, BusinessDetails, NormalMode, UniqueTaxpayerReference, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.organisation.{OrganisationRegistrationTypePage, UniqueTaxpayerReferenceInUserAnswers, WhatIsTheNameOfYourBusinessPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.RegistrationService
import views.html.organisation.WhatIsTheNameOfYourBusinessView

import scala.concurrent.Future

class WhatIsTheNameOfYourBusinessControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  private def getBusinessTypeMessageKey(userAnswers: UserAnswers): String =
    userAnswers.get(OrganisationRegistrationTypePage) match {
      case Some(LimitedCompany) | Some(LLP) => "whatIsTheNameOfYourBusiness.ltdLpLlp"
      case Some(Partnership)                => "whatIsTheNameOfYourBusiness.partnership"
      case _                                => "whatIsTheNameOfYourBusiness.unincorporatedAssociationTrust"
    }

  lazy val whatIsTheNameOfYourBusinessRoute: String =
    controllers.organisation.routes.WhatIsTheNameOfYourBusinessController.onPageLoad(NormalMode).url

  final val mockRegistrationService: RegistrationService = mock[RegistrationService]

  val testBusinessDetails: BusinessDetails = BusinessDetails(
    name = "Test Company Ltd",
    address = Address("1 Test Road", None, None, None, Some("LU4 1ST"), "GB")
  )

  "WhatIsTheNameOfYourBusiness Controller" - {

    "must return OK and the correct view for a GET when option is Ltd or LLP" in {

      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, LimitedCompany)
        .success
        .value

      val businessType = getBusinessTypeMessageKey(userAnswers)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))

      running(application) {
        val request = FakeRequest(GET, whatIsTheNameOfYourBusinessRoute)

        val result = route(application, request).value
        val form   = new WhatIsTheNameOfYourBusinessFormProvider().apply(
          businessType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )

        val view = application.injector.instanceOf[WhatIsTheNameOfYourBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, businessType)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when option is Partnership" in {

      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, Partnership)
        .success
        .value

      val businessType = getBusinessTypeMessageKey(userAnswers)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))

      running(application) {
        val request = FakeRequest(GET, whatIsTheNameOfYourBusinessRoute)

        val result = route(application, request).value
        val form   = new WhatIsTheNameOfYourBusinessFormProvider().apply(
          businessType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )

        val view = application.injector.instanceOf[WhatIsTheNameOfYourBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, businessType)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET when option is unincorporatedAssociationTrust" in {

      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, Trust)
        .success
        .value

      val businessType = getBusinessTypeMessageKey(userAnswers)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))

      running(application) {
        val request = FakeRequest(GET, whatIsTheNameOfYourBusinessRoute)

        val result = route(application, request).value
        val form   = new WhatIsTheNameOfYourBusinessFormProvider().apply(
          businessType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )

        val view = application.injector.instanceOf[WhatIsTheNameOfYourBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, businessType)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val companyName = "Example Company Ltd"
      val userAnswers = UserAnswers(userAnswersId)
        .set(OrganisationRegistrationTypePage, LLP)
        .success
        .value
        .set(WhatIsTheNameOfYourBusinessPage, companyName)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))

      running(application) {
        val request      = FakeRequest(GET, whatIsTheNameOfYourBusinessRoute)
        val form         = new WhatIsTheNameOfYourBusinessFormProvider().apply(
          businessType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )
        val view         = application.injector.instanceOf[WhatIsTheNameOfYourBusinessView]
        val businessType = getBusinessTypeMessageKey(userAnswers)

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(companyName), NormalMode, businessType)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val companyName = "Test Company Ltd"

      val initialUserAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, LimitedCompany)
        .success
        .value
        .set(UniqueTaxpayerReferenceInUserAnswers, testUtr)
        .success
        .value

      val finalUserAnswers = initialUserAnswers
        .set(WhatIsTheNameOfYourBusinessPage, companyName)
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(
        mockRegistrationService.getBusinessWithUserInput(
          eqTo(finalUserAnswers)
        )(any())
      ) thenReturn Future.successful(Some(testBusinessDetails))

      val application =
        applicationBuilder(userAnswers = Some(initialUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[RegistrationService].toInstance(mockRegistrationService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatIsTheNameOfYourBusinessRoute)
            .withFormUrlEncodedBody(("value", companyName))

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

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[RegistrationService].toInstance(mockRegistrationService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(POST, whatIsTheNameOfYourBusinessRoute)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whatIsTheNameOfYourBusinessRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whatIsTheNameOfYourBusinessRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
