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
import forms.organisation.OrganisationSecondContactPhoneNumberFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import pages.organisation.{OrganisationSecondContactNamePage, OrganisationSecondContactPhoneNumberPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.OrganisationSecondContactPhoneNumberView

import scala.concurrent.Future

class OrganisationSecondContactPhoneNumberControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider: OrganisationSecondContactPhoneNumberFormProvider =
    new OrganisationSecondContactPhoneNumberFormProvider()
  val form: Form[String]                                             = formProvider()

  lazy val organisationSecondContactPhoneNumberRoute: String =
    controllers.organisation.routes.OrganisationSecondContactPhoneNumberController.onPageLoad(NormalMode).url

  val userAnswersWithName: UserAnswers =
    emptyUserAnswers.set(OrganisationSecondContactNamePage, "Simothy").success.value

  "OrganisationSecondContactPhoneNumber Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithName)).build()

      running(application) {
        val request = FakeRequest(GET, organisationSecondContactPhoneNumberRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationSecondContactPhoneNumberView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, "Simothy")(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val prePopulatedUserAnswers = UserAnswers(userAnswersId)
        .set(OrganisationSecondContactNamePage, "Simothy")
        .success
        .value
        .set(OrganisationSecondContactPhoneNumberPage, "12345")
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(prePopulatedUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, organisationSecondContactPhoneNumberRoute)

        val view = application.injector.instanceOf[OrganisationSecondContactPhoneNumberView]

        val result = route(application, request).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill("12345"), NormalMode, "Simothy")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithName))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, organisationSecondContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", "07123456789"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithName)).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationSecondContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[OrganisationSecondContactPhoneNumberView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, "Simothy")(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing contact name is found" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, organisationSecondContactPhoneNumberRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing contact name is found in form with errors" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationSecondContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", "invalid answer"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, organisationSecondContactPhoneNumberRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, organisationSecondContactPhoneNumberRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
