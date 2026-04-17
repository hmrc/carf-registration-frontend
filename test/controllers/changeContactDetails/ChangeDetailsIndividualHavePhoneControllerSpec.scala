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
import forms.ChangeDetailsIndividualHavePhoneFormProvider
import models.{NormalMode, ProvideMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.changeContactDetails.ChangeDetailsIndividualHavePhonePage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ChangeDetailsIndividualHavePhoneView

import scala.concurrent.Future

class ChangeDetailsIndividualHavePhoneControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val changeDetailsIndividualHavePhoneRoute: String =
    controllers.changeContactDetails.routes.ChangeDetailsIndividualHavePhoneController.onPageLoad(NormalMode).url

  lazy val changeDetailsIndividualHavePhoneProvideRoute: String =
    controllers.changeContactDetails.routes.ChangeDetailsIndividualHavePhoneController.onPageLoad(ProvideMode).url

  val formProvider        = new ChangeDetailsIndividualHavePhoneFormProvider()
  val form: Form[Boolean] = formProvider()

  "ChangeDetailsIndividualHavePhone Controller" - {

    "must return OK and the correct view for a GET in NormalMode" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsIndividualHavePhoneRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChangeDetailsIndividualHavePhoneView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must return OK and the correct view for a GET in ProvideMode" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsIndividualHavePhoneProvideRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChangeDetailsIndividualHavePhoneView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, ProvideMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(ChangeDetailsIndividualHavePhonePage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsIndividualHavePhoneRoute)

        val view = application.injector.instanceOf[ChangeDetailsIndividualHavePhoneView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" - {
      "when changing from Yes -> Yes, redirect via navigator in NormalMode" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(ChangeDetailsIndividualHavePhonePage, true)
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
            FakeRequest(POST, changeDetailsIndividualHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "when changing from Yes -> Yes, redirect via navigator in ProvideMode" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(ChangeDetailsIndividualHavePhonePage, true)
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
            FakeRequest(POST, changeDetailsIndividualHavePhoneProvideRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "when changing from Yes -> No, redirect via navigator" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(ChangeDetailsIndividualHavePhonePage, true)
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
            FakeRequest(POST, changeDetailsIndividualHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockSessionRepository).set(argThat(_.get(ChangeDetailsIndividualHavePhonePage).get == false))
        }
      }

      "when changing from No -> No, redirect via navigator" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(ChangeDetailsIndividualHavePhonePage, false)
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
            FakeRequest(POST, changeDetailsIndividualHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockSessionRepository).set(argThat(_.get(ChangeDetailsIndividualHavePhonePage).get == false))
        }
      }

      "when changing from No -> Yes, redirect to ChangeDetailsIndividualPhoneNumberPage in NormalMode" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(ChangeDetailsIndividualHavePhonePage, false)
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
            FakeRequest(POST, changeDetailsIndividualHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(
            result
          ).value        mustEqual controllers.changeContactDetails.routes.ChangeIndividualPhoneNumberController
            .onPageLoad(NormalMode)
            .url
          verify(mockSessionRepository).set(argThat(_.get(ChangeDetailsIndividualHavePhonePage).get == true))
        }
      }

      "when changing from No -> Yes, redirect to ChangeDetailsIndividualPhoneNumberPage in ProvideMode" in {
        val userAnswers = UserAnswers(userAnswersId)
          .set(ChangeDetailsIndividualHavePhonePage, false)
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
            FakeRequest(POST, changeDetailsIndividualHavePhoneProvideRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(
            result
          ).value        mustEqual controllers.changeContactDetails.routes.ChangeIndividualPhoneNumberController
            .onPageLoad(ProvideMode)
            .url
          verify(mockSessionRepository).set(argThat(_.get(ChangeDetailsIndividualHavePhonePage).get == true))
        }
      }

      "when old value is empty, redirect to JourneyRecoveryPage" in {
        val application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, changeDetailsIndividualHavePhoneRoute)
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsIndividualHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[ChangeDetailsIndividualHavePhoneView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsIndividualHavePhoneRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsIndividualHavePhoneRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
