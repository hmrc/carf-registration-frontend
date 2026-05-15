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

package controllers

import base.SpecBase
import forms.organisation.OrganisationSecondContactPhoneNumberFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.changeContactDetails.{ChangeDetailsOrgSecondNamePage, ChangeDetailsOrgSecondPhoneNumberPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ChangeOrgSecondContactPhoneNumberView

import scala.concurrent.Future

class ChangeOrgSecondContactPhoneNumberControllerSpec extends SpecBase with MockitoSugar {

  val formProvider: OrganisationSecondContactPhoneNumberFormProvider =
    new OrganisationSecondContactPhoneNumberFormProvider()
  val form: Form[String]                                             = formProvider()
  val validPhoneNumber: String                                       = "07987654321"
  val validPhoneNumbers: List[String]                                = List("07987654321", "+447987654321", "")
  def onwardRoute                                                    = Call("GET", "/register-for-cryptoasset-reporting/problem/contact-details-are-missing")

  lazy val changeOrgSecondContactPhoneNumberRoute =
    changeContactDetails.routes.ChangeOrgSecondContactPhoneNumberController.onPageLoad().url

  "ChangeOrgSecondContactPhoneNumber Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = UserAnswers(userAnswersId).withPage(ChangeDetailsOrgSecondNamePage, "John Doe")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeOrgSecondContactPhoneNumberRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ChangeOrgSecondContactPhoneNumberView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, "John Doe")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .withPage(
          ChangeDetailsOrgSecondPhoneNumberPage,
          validPhoneNumber
        )
        .withPage(ChangeDetailsOrgSecondNamePage, "John Doe")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeOrgSecondContactPhoneNumberRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[ChangeOrgSecondContactPhoneNumberView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validPhoneNumber), NormalMode, "John Doe")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information Is Missing Page when name cannot be found in the session for GET" in {
      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgSecondPhoneNumberPage, validPhoneNumber)

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeOrgSecondContactPhoneNumberRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeContactDetails.routes.ContactDetailsMissingController
          .onPageLoad()
          .url
      }
    }

    "must redirect to Some Information Is Missing Page when name cannot be found in the session for POST when invalid data is submitted" in {
      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgSecondPhoneNumberPage, validPhoneNumber)

      userAnswers.get(ChangeDetailsOrgSecondNamePage) mustBe None

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeOrgSecondContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.changeContactDetails.routes.ContactDetailsMissingController
          .onPageLoad()
          .url
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeOrgSecondContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", validPhoneNumber))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrgSecondNamePage, "John Doe")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeOrgSecondContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view      = application.injector.instanceOf[ChangeOrgSecondContactPhoneNumberView]
        val result    = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, "John Doe")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, changeOrgSecondContactPhoneNumberRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, changeOrgSecondContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", validPhoneNumber.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
