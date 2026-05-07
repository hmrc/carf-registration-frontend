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
import forms.organisation.OrganisationHaveSecondContactFormProvider
import models.{NormalMode, ProvideMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.changeContactDetails.{ChangeDetailsFirstContactNamePage, ChangeDetailsOrganisationHaveSecondContactPage, ChangeDetailsOrganisationSecondContactEmailPage, ChangeDetailsOrganisationSecondContactNamePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ChangeDetailsOrganisationHaveSecondContactView

import scala.concurrent.Future

class ChangeDetailsOrganisationHaveSecondContactControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new OrganisationHaveSecondContactFormProvider()
  val form: Form[Boolean] = formProvider()

  lazy val changeDetailsOrganisationHaveSecondContactRoute: String =
    controllers.changeContactDetails.routes.ChangeDetailsOrganisationHaveSecondContactController
      .onPageLoad()
      .url

  "ChangeDetailsOrganisationHaveSecondContact Controller" - {

    "must return OK and the correct view for a GET when name is provided" in {

      val application = applicationBuilder(userAnswers =
        Some(emptyUserAnswers.withPage(ChangeDetailsFirstContactNamePage, "John Smith"))
      ).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsOrganisationHaveSecondContactRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChangeDetailsOrganisationHaveSecondContactView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, "John Smith")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .withPage(ChangeDetailsOrganisationHaveSecondContactPage, true)
        .withPage(ChangeDetailsFirstContactNamePage, "John Smith")

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsOrganisationHaveSecondContactRoute)

        val view = application.injector.instanceOf[ChangeDetailsOrganisationHaveSecondContactView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, "John Smith")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to placeholder when the existing value is yes and the new value is yes" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers.withPage(ChangeDetailsOrganisationHaveSecondContactPage, true)
      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrganisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.routes.PlaceholderController
            .onPageLoad("Should redirect to change-contact/organisation/details page (CARF-141)")
            .url
      }
    }

    "must redirect according to the navigator when the existing value is no and the new value is no" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers.withPage(ChangeDetailsOrganisationHaveSecondContactPage, false)
      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrganisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to second contact name page when the existing value is no and the new value is yes" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers.withPage(ChangeDetailsOrganisationHaveSecondContactPage, false)
      val application =
        applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrganisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value        mustEqual controllers.changeContactDetails.routes.ChangeDetailsOrganisationSecondContactNameController
          .onPageLoad(ProvideMode)
          .url
      }
    }

    "must remove second contact name data when answer is no" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = emptyUserAnswers
        .withPage(ChangeDetailsOrganisationHaveSecondContactPage, true)
        .withPage(ChangeDetailsOrganisationSecondContactNamePage, "Prof. Birch")
      // TODO: Update this test when CARF-192 / CARF-193 are implemented, to also remove the second contact email / phone related pages.

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrganisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(argThat { ua =>
          ua.get(ChangeDetailsOrganisationSecondContactNamePage).isEmpty &&
          ua.get(ChangeDetailsOrganisationSecondContactEmailPage).isEmpty &&
          ua.get(ChangeDetailsOrganisationHaveSecondContactPage).contains(false)
        })
      }
    }

    "must return a Bad Request and errors when invalid data is submitted and first contact name is provided" in {

      val application = applicationBuilder(userAnswers =
        Some(emptyUserAnswers.withPage(ChangeDetailsFirstContactNamePage, "John Smith"))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrganisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ChangeDetailsOrganisationHaveSecondContactView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, "John Smith")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Some Information Is Missing Page when name cannot be found in the session for GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, changeDetailsOrganisationHaveSecondContactRoute)
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
          FakeRequest(POST, changeDetailsOrganisationHaveSecondContactRoute)
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
        val request = FakeRequest(GET, changeDetailsOrganisationHaveSecondContactRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, changeDetailsOrganisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
