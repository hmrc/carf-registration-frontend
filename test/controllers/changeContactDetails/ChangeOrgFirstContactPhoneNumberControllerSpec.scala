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
import controllers.{changeContactDetails, routes}
import forms.organisation.FirstContactPhoneNumberFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.changeContactDetails.{ChangeDetailsOrgFirstNamePage, ChangeDetailsOrgFirstPhoneNumberPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ChangeOrgFirstContactPhoneNumberView

import scala.concurrent.Future

class ChangeOrgFirstContactPhoneNumberControllerSpec extends SpecBase with MockitoSugar {

  val formProvider: FirstContactPhoneNumberFormProvider = new FirstContactPhoneNumberFormProvider()
  val form: Form[String]                                = formProvider()
  def onwardRoute: Call                                 = Call("GET", "/foo")
  val validPhoneNumber: String                          = "07777777777"

  lazy val changeFirstContactPhoneNumberRoute: String =
    changeContactDetails.routes.ChangeOrgFirstContactPhoneNumberController.onPageLoad().url

  "ChangeFirstContactPhoneNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgFirstNamePage, "John Smith")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeFirstContactPhoneNumberRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ChangeOrgFirstContactPhoneNumberView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, "John Smith")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgFirstPhoneNumberPage, validPhoneNumber)
        .withPage(ChangeDetailsOrgFirstNamePage, "John Smith")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeFirstContactPhoneNumberRoute)
        val view    = application.injector.instanceOf[ChangeOrgFirstContactPhoneNumberView]
        val result  = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validPhoneNumber), NormalMode, "John Smith")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information Is Missing Page when name cannot be found in the session for GET" in {
      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgFirstPhoneNumberPage, validPhoneNumber)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeFirstContactPhoneNumberRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeContactDetails.routes.ContactDetailsMissingController
          .onPageLoad()
          .url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgFirstNamePage, "John Smith")

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeFirstContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", validPhoneNumber))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgFirstNamePage, "John Smith")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeFirstContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view      = application.injector.instanceOf[ChangeOrgFirstContactPhoneNumberView]
        val result    = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, "John Smith")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information Is Missing Page when name cannot be found in the session for POST when invalid data is submitted" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = UserAnswers(userAnswersId)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeFirstContactPhoneNumberRoute)
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
        val request = FakeRequest(GET, changeFirstContactPhoneNumberRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, changeFirstContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", validPhoneNumber))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
