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
import forms.organisation.OrganisationHaveSecondContactFormProvider
import models.JourneyType.OrgWithUtr
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.organisation.{FirstContactNamePage, OrganisationHaveSecondContactPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.organisation.OrganisationHaveSecondContactView

import java.time.Clock
import scala.concurrent.Future

class OrganisationHaveSecondContactControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider: OrganisationHaveSecondContactFormProvider = new OrganisationHaveSecondContactFormProvider()
  val form: Form[Boolean]                                     = formProvider()
  val firstContactName: String                                = "Contact Name"

  lazy val organisationHaveSecondContactRoute: String =
    controllers.organisation.routes.OrganisationHaveSecondContactController.onPageLoad(NormalMode).url

  "OrganisationHaveSecondContact Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(FirstContactNamePage, firstContactName)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, organisationHaveSecondContactRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationHaveSecondContactView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, firstContactName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(OrganisationHaveSecondContactPage, true)
        .success
        .value
        .set(FirstContactNamePage, firstContactName)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, organisationHaveSecondContactRoute)

        val view = application.injector.instanceOf[OrganisationHaveSecondContactView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, firstContactName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val expectedUserAnswers = emptyUserAnswers.set(OrganisationHaveSecondContactPage, true).success.value

      when(mockSessionRepository.set(eqTo(expectedUserAnswers))) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[Clock].toInstance(clock)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, organisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
        verify(mockSessionRepository, times(1)).set(eqTo(expectedUserAnswers))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(FirstContactNamePage, firstContactName)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[OrganisationHaveSecondContactView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, firstContactName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, organisationHaveSecondContactRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no first contact name is found" in {

      val userAnswers = UserAnswers(userAnswersId)
        .set(OrganisationHaveSecondContactPage, true)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, organisationHaveSecondContactRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no first contact name is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, organisationHaveSecondContactRoute)
            .withFormUrlEncodedBody(("value", "invalid"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }
}
