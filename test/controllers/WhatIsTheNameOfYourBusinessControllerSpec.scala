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
import forms.WhatIsTheNameOfYourBusinessFormProvider
import models.OrganisationRegistrationType.*
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.{OrganisationRegistrationTypePage, WhatIsTheNameOfYourBusinessPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.WhatIsTheNameOfYourBusinessView

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
    routes.WhatIsTheNameOfYourBusinessController.onPageLoad(NormalMode).url

  "WhatIsTheNameOfYourBusiness Controller" - {

    "must return OK and the correct view for a GET" in {

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

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(OrganisationRegistrationTypePage, LLP)
        .success
        .value
        .set(WhatIsTheNameOfYourBusinessPage, "Example.Limited.Company Ltd")
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
        contentAsString(result) mustEqual view(form.fill("Example.Limited.Company Ltd"), NormalMode, businessType)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val userAnswers = emptyUserAnswers
        .set(OrganisationRegistrationTypePage, LimitedCompany)
        .success
        .value

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatIsTheNameOfYourBusinessRoute)
            .withFormUrlEncodedBody(("value", "answer"))

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
          FakeRequest(POST, whatIsTheNameOfYourBusinessRoute)
            .withFormUrlEncodedBody(("value", ""))

        val form = new WhatIsTheNameOfYourBusinessFormProvider().apply(
          businessType = userAnswers.get(OrganisationRegistrationTypePage).get.toString
        )

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhatIsTheNameOfYourBusinessView]

        val businessTypeMessageKey = getBusinessTypeMessageKey(userAnswers)

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, businessTypeMessageKey)(
          request,
          messages(application)
        ).toString
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
