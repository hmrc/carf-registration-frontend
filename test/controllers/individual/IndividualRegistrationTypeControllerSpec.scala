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
import forms.individual.IndividualRegistrationTypeFormProvider
import models.IndividualRegistrationType.{IndividualNotConnectedToABusiness, IndividualSoleTrader}
import models.{IndividualRegistrationType, NormalMode, RegistrationType, SafeId, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.RegistrationTypePage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.individual.IndividualRegistrationTypeView

import scala.concurrent.Future

class IndividualRegistrationTypeControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val individualRegistrationTypeRoute =
    controllers.individual.routes.IndividualRegistrationTypeController.onPageLoad(NormalMode).url

  val formProvider = new IndividualRegistrationTypeFormProvider()
  val form         = formProvider()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository)
  }

  "IndividualRegistrationType Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, individualRegistrationTypeRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[IndividualRegistrationTypeView]
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(RegistrationTypePage, RegistrationType.SoleTrader)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, individualRegistrationTypeRoute)
        val view    = application.injector.instanceOf[IndividualRegistrationTypeView]
        val result  = route(application, request).value
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(IndividualRegistrationType.values.head), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must not prepopulate the view on a GET when the question has previously been answered with an organisation registration type" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(RegistrationTypePage, RegistrationType.LimitedCompany)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, individualRegistrationTypeRoute)
        val view    = application.injector.instanceOf[IndividualRegistrationTypeView]
        val result  = route(application, request).value
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" - {
      "when the answer has not changed" - {
        "when match flag is true and safeId has been set" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val userAnswers = emptyUserAnswers
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
            .withPage(RegistrationTypePage, RegistrationType.SoleTrader)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = Individual)
              .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, individualRegistrationTypeRoute)
                .withFormUrlEncodedBody(("individualRegistrationType", IndividualSoleTrader.toString))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository, times(1)).set(eqTo(userAnswers))
          }
        }

        "when match flag is false and safeId has not been set" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val userAnswers = emptyUserAnswers.withPage(RegistrationTypePage, RegistrationType.Individual)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = Individual)
              .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, individualRegistrationTypeRoute)
                .withFormUrlEncodedBody(("individualRegistrationType", IndividualNotConnectedToABusiness.toString))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository, times(1)).set(eqTo(userAnswers))
          }
        }
      }

      "when the answer has changed" - {
        "when RegistrationTypePage was not previously populated" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val application =
            applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = Individual)
              .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, individualRegistrationTypeRoute)
                .withFormUrlEncodedBody(("individualRegistrationType", IndividualSoleTrader.toString))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository, times(1)).set(
              eqTo(emptyUserAnswers.withPage(RegistrationTypePage, RegistrationType.SoleTrader))
            )
          }
        }

        "when RegistrationType changes from SoleTrader to Individual, does not clear match flag and safeId" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val userAnswers = emptyUserAnswers
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
            .withPage(RegistrationTypePage, RegistrationType.SoleTrader)

          val expectedSetUserAnswers = emptyUserAnswers
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
            .withPage(RegistrationTypePage, RegistrationType.Individual)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = Individual)
              .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, individualRegistrationTypeRoute)
                .withFormUrlEncodedBody(("individualRegistrationType", IndividualNotConnectedToABusiness.toString))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository, times(1)).set(eqTo(expectedSetUserAnswers))
          }
        }

        "when RegistrationType changes from Individual to SoleTrader, does not clear match flag and safeId" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val userAnswers = emptyUserAnswers
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
            .withPage(RegistrationTypePage, RegistrationType.Individual)

          val expectedSetUserAnswers = emptyUserAnswers
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
            .withPage(RegistrationTypePage, RegistrationType.SoleTrader)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = Individual)
              .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, individualRegistrationTypeRoute)
                .withFormUrlEncodedBody(("individualRegistrationType", IndividualSoleTrader.toString))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository, times(1)).set(eqTo(expectedSetUserAnswers))
          }
        }

        "when RegistrationType changes from LimitedCompany to SoleTrader, clears match flag and safeId" in {
          when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

          val userAnswers = emptyUserAnswers
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
            .withPage(RegistrationTypePage, RegistrationType.LimitedCompany)

          val expectedSetUserAnswers = emptyUserAnswers.withPage(RegistrationTypePage, RegistrationType.SoleTrader)

          val application =
            applicationBuilder(userAnswers = Some(userAnswers), affinityGroup = Individual)
              .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
              .build()

          running(application) {
            val request =
              FakeRequest(POST, individualRegistrationTypeRoute)
                .withFormUrlEncodedBody(("individualRegistrationType", IndividualSoleTrader.toString))
            val result  = route(application, request).value

            status(result)                 mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual onwardRoute.url

            verify(mockSessionRepository, times(1)).set(eqTo(expectedSetUserAnswers))
          }
        }
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request   =
          FakeRequest(POST, individualRegistrationTypeRoute)
            .withFormUrlEncodedBody(("individualRegistrationType", "invalid value"))
        val boundForm = form.bind(Map("individualRegistrationType" -> "invalid value"))
        val view      = application.injector.instanceOf[IndividualRegistrationTypeView]
        val result    = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(GET, individualRegistrationTypeRoute)
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request =
          FakeRequest(POST, individualRegistrationTypeRoute)
            .withFormUrlEncodedBody(("value", IndividualRegistrationType.values.head.toString))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
