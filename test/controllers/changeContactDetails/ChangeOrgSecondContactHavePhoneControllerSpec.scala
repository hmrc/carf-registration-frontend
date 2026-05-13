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

package controllers.changeContactDetails

import base.SpecBase
import controllers.routes
import forms.organisation.OrganisationSecondContactHavePhoneFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.changeContactDetails.{ChangeDetailsOrgSecondHavePhonePage, ChangeDetailsOrgSecondNamePage, ChangeDetailsOrgSecondPhoneNumberPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ChangeOrgSecondContactHavePhoneView

import scala.concurrent.Future

class ChangeOrgSecondContactHavePhoneControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new OrganisationSecondContactHavePhoneFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val changeDetailsOrgSecondContactHavePhoneRoute: String =
    controllers.changeContactDetails.routes.ChangeOrgSecondContactHavePhoneController
      .onPageLoad(NormalMode)
      .url

  "ChangeOrgSecondContactHavePhoneController" - {

    "must return OK and the correct view for a GET when second contact name is provided" in {

      val application = applicationBuilder(userAnswers =
        Some(emptyUserAnswers.withPage(ChangeDetailsOrgSecondNamePage, "Prof. Birch"))
      ).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsOrgSecondContactHavePhoneRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ChangeOrgSecondContactHavePhoneView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, "Prof. Birch")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgSecondHavePhonePage, true)
        .withPage(ChangeDetailsOrgSecondNamePage, "Prof. Birch")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsOrgSecondContactHavePhoneRoute)
        val view    = application.injector.instanceOf[ChangeOrgSecondContactHavePhoneView]
        val result  = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, "Prof. Birch")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect according to the navigator when the existing value is yes and the new value is yes" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .withPage(ChangeDetailsOrgSecondHavePhonePage, true)
        .withPage(ChangeDetailsOrgSecondNamePage, "Prof. Birch")

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrgSecondContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect according to the navigator when the answer is no" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .withPage(ChangeDetailsOrgSecondHavePhonePage, true)
        .withPage(ChangeDetailsOrgSecondNamePage, "Prof. Birch")

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrgSecondContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must remove second contact phone number when answer is no" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .withPage(ChangeDetailsOrgSecondNamePage, "Prof. Birch")
        .withPage(ChangeDetailsOrgSecondHavePhonePage, true)
        .withPage(ChangeDetailsOrgSecondPhoneNumberPage, "07777777777")

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrgSecondContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(argThat { ua =>
          ua.get(ChangeDetailsOrgSecondPhoneNumberPage).isEmpty &&
          ua.get(ChangeDetailsOrgSecondHavePhonePage).contains(false)
        })
      }
    }

    "must redirect to second contact phone page when existing value is no and new value is yes" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .withPage(ChangeDetailsOrgSecondHavePhonePage, false)
        .withPage(ChangeDetailsOrgSecondNamePage, "Prof. Birch")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrgSecondContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value        mustEqual controllers.changeContactDetails.routes.ChangeOrgSecondContactPhoneNumberController
          .onPageLoad()
          .url

      }
    }

    "must redirect to second contact phone page when existing value is missing and new value is yes" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .withPage(ChangeDetailsOrgSecondNamePage, "Prof. Birch")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrgSecondContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value        mustEqual controllers.changeContactDetails.routes.ChangeOrgSecondContactPhoneNumberController
          .onPageLoad()
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted and second contact name is provided" in {

      val application = applicationBuilder(userAnswers =
        Some(emptyUserAnswers.withPage(ChangeDetailsOrgSecondNamePage, "Prof. Birch"))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrgSecondContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[ChangeOrgSecondContactHavePhoneView]
        val result    = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, "Prof. Birch")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information Is Missing Page when name cannot be found in the session for GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsOrgSecondContactHavePhoneRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeContactDetails.routes.ContactDetailsMissingController
          .onPageLoad()
          .url
      }
    }

    "must redirect to Some Information Is Missing Page when name cannot be found in the session for POST when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrgSecondContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeContactDetails.routes.ContactDetailsMissingController
          .onPageLoad()
          .url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsOrgSecondContactHavePhoneRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrgSecondContactHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
