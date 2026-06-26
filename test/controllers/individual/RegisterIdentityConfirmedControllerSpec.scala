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
import models.{ChangeMode, NormalMode, ProvideMode}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.IndividualEmailPage
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.individual.RegisterIdentityConfirmedView

class RegisterIdentityConfirmedControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call                                 = Call("GET", "/foo")
  lazy val registerIdentityConfirmedRoute: String       =
    controllers.individual.routes.RegisterIdentityConfirmedController.onPageLoad(NormalMode).url
  lazy val changeRegisterIdentityConfirmedRoute: String =
    controllers.individual.routes.RegisterIdentityConfirmedController.onPageLoad(ChangeMode).url

  "RegisterIdentityConfirmed Controller" - {
    "Normal mode" - {
      "must return OK and the correct continue url when individual email is populated" in {
        val application = applicationBuilder(userAnswers =
          Some(
            emptyUserAnswers
              .withPage(IndividualEmailPage, testEmail)
          )
        ).build()

        running(application) {
          val request     = FakeRequest(GET, registerIdentityConfirmedRoute)
          val result      = route(application, request).value
          val view        = application.injector.instanceOf[RegisterIdentityConfirmedView]
          val continueUrl =
            controllers.routes.CheckYourAnswersController.onPageLoad().url

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(application)).toString
        }
      }

      "must return OK and the correct continue url when individual email is NOT populated" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request     = FakeRequest(GET, registerIdentityConfirmedRoute)
          val result      = route(application, request).value
          val view        = application.injector.instanceOf[RegisterIdentityConfirmedView]
          val continueUrl =
            controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode).url

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(application)).toString
        }
      }
    }

    "Change mode" - {
      "must return OK and the correct continue url when individual email is populated" in {
        val application = applicationBuilder(userAnswers =
          Some(
            emptyUserAnswers
              .withPage(IndividualEmailPage, testEmail)
          )
        ).build()

        running(application) {
          val request     = FakeRequest(GET, changeRegisterIdentityConfirmedRoute)
          val result      = route(application, request).value
          val view        = application.injector.instanceOf[RegisterIdentityConfirmedView]
          val continueUrl =
            controllers.routes.CheckYourAnswersController.onPageLoad().url

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(application)).toString
        }
      }

      "must return OK and the correct continue url when individual email is NOT populated" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request     = FakeRequest(GET, changeRegisterIdentityConfirmedRoute)
          val result      = route(application, request).value
          val view        = application.injector.instanceOf[RegisterIdentityConfirmedView]
          val continueUrl =
            controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode).url

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(continueUrl)(request, messages(application)).toString
        }
      }
    }
  }
}
