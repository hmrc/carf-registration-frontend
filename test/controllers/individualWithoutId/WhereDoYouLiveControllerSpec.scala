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

package controllers.individualWithoutId

import base.SpecBase
import controllers.routes
import forms.{IsThisYourBusinessFormProvider, WhereDoYouLiveFormProvider}
import models.*
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import pages.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.RegistrationService
import views.html.{IsThisYourBusinessView, WhereDoYouLiveView}

import scala.concurrent.Future

class WhereDoYouLiveControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {

  def onwardRoute: Call = Call("GET", "/foo")

  lazy private val whereDoYouLiveRouteGet: String  =
    controllers.individualWithoutId.routes.WhereDoYouLiveController.onPageLoad(NormalMode).url
  lazy private val whereDoYouLiveRoutePost: String =
    controllers.individualWithoutId.routes.WhereDoYouLiveController.onSubmit(NormalMode).url

  "WhereDoYouLiveController" - {
    "on a Sole Trader without ID journey" - {
      "must return OK and the WhereDoYouLive View" in {
        val application = applicationBuilder(userAnswers = Option(emptyUserAnswers)).build()

        running(application) {
          val form    = new WhereDoYouLiveFormProvider().apply()
          val request = FakeRequest(GET, whereDoYouLiveRouteGet)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[WhereDoYouLiveView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {
        val application = applicationBuilder(userAnswers =
          Option(
            emptyUserAnswers
              .set(WhereDoYouLivePage, true)
              .success
              .value
          )
        ).build()

        running(application) {
          val form    = new WhereDoYouLiveFormProvider().apply()
          val request = FakeRequest(GET, whereDoYouLiveRouteGet)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[WhereDoYouLiveView]

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, messages(application)).toString
        }
      }
    }

    "onSubmit" - {
      "must redirect to the Find your address page when yes is submitted" in {
        val setUserAnswers = Option(UserAnswers(userAnswersId).set(WhereDoYouLivePage, true).success.value)

        val application = applicationBuilder(userAnswers = setUserAnswers)
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        running(application) {
          val request = FakeRequest(POST, whereDoYouLiveRoutePost).withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
          verify(mockSessionRepository).set(any)
        }
      }

      "must return a Bad Request when invalid data is submitted" in {
        val setUserAnswers = Option(UserAnswers(userAnswersId))

        val application = applicationBuilder(userAnswers = setUserAnswers)
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
          .build()

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        running(application) {
          val request = FakeRequest(POST, whereDoYouLiveRoutePost).withFormUrlEncodedBody(("value", ""))
          val result  = route(application, request).value

          status(result) mustEqual BAD_REQUEST
        }
      }

      "must return redirect to Journey recovery if user answers is not present" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, whereDoYouLiveRouteGet)
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey recovery when user answers is empty (None)" in {
        val unsetUserAnswers = None

        val application = applicationBuilder(userAnswers = unsetUserAnswers).build()

        running(application) {
          val request = FakeRequest(POST, whereDoYouLiveRoutePost).withFormUrlEncodedBody(("value", "true"))
          val result  = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
