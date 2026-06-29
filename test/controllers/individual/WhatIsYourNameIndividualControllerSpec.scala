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

package controllers.individual

import base.SpecBase
import controllers.routes
import forms.individual.WhatIsYourNameIndividualFormProvider
import models.{Name, NormalMode, SafeId, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import pages.individual.WhatIsYourNameIndividualPage
import play.api.data.Form
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.individual.WhatIsYourNameIndividualView

import scala.concurrent.Future

class WhatIsYourNameIndividualControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider     = new WhatIsYourNameIndividualFormProvider()
  val form: Form[Name] = formProvider()

  lazy val whatIsYourNameIndividualRoute: String =
    controllers.individual.routes.WhatIsYourNameIndividualController.onPageLoad(NormalMode).url

  val userAnswers = UserAnswers(
    id = userAnswersId,
    data = Json.obj(
      WhatIsYourNameIndividualPage.toString -> Json.obj(
        "firstName" -> "firstName example",
        "lastName"  -> "lastName example"
      )
    )
  )

  "Normal mode" - {
    "WhatIsYourNameIndividual Controller" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, whatIsYourNameIndividualRoute)

          val view = application.injector.instanceOf[WhatIsYourNameIndividualView]

          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers: UserAnswers =
          emptyUserAnswers.withPage(WhatIsYourNameIndividualPage, Name("firstName", "lastName"))

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, whatIsYourNameIndividualRoute)

          val view = application.injector.instanceOf[WhatIsYourNameIndividualView]

          val result = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form.fill(Name("firstName", "lastName")), NormalMode)(
            request,
            messages(application)
          ).toString
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
            FakeRequest(POST, whatIsYourNameIndividualRoute)
              .withFormUrlEncodedBody(("firstName", "firstName timmy"), ("lastName", "lastName timmerson"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, whatIsYourNameIndividualRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[WhatIsYourNameIndividualView]

          val result = route(application, request).value

          status(result)          mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, whatIsYourNameIndividualRoute)

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, whatIsYourNameIndividualRoute)
              .withFormUrlEncodedBody(("firstName", "value 1"), ("lastName", "value 2"))

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }

  "Change Mode" - {
    "must redirect to the next page and clear match flag if changed" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers =
          Some(
            emptyUserAnswers
              .copy(hasValidMatch = true, safeId = Some(SafeId("XCARF000000001")))
              .withPage(WhatIsYourNameIndividualPage, Name("firstName", "lastName"))
          )
        )
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatIsYourNameIndividualRoute)
            .withFormUrlEncodedBody(("firstName", "newName timmy"), ("lastName", "lastName"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(
          argThat(ua => !ua.hasValidMatch && ua.safeId.isEmpty)
        )
      }
    }

    "must redirect to the next page and not clear match flag if not changed" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers =
          Some(
            emptyUserAnswers
              .copy(hasValidMatch = true, safeId = Some(SafeId("XCARF000000001")))
              .withPage(WhatIsYourNameIndividualPage, Name("firstName", "lastName"))
          )
        )
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatIsYourNameIndividualRoute)
            .withFormUrlEncodedBody(("firstName", "firstName"), ("lastName", "lastName"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        verify(mockSessionRepository).set(
          argThat(ua => ua.hasValidMatch && ua.safeId.isDefined)
        )
      }
    }
  }
}
