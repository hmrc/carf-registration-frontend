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

package controllers.changeContactDetails

import base.SpecBase
import models.{UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{times, verify, when}
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualPhoneNumberPage}
import pages.individual.HaveNiNumberPage
import pages.organisation.UniqueTaxpayerReferenceInUserAnswers
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SubscriptionService

import scala.concurrent.Future

class ChangeContactDetailsIndexControllerSpec extends SpecBase {

  val testExistingUserAnswers: UserAnswers        =
    UserAnswers(id = "test-id", lastUpdated = clock.instant()).set(HaveNiNumberPage, true).success.value
  val testExistingUserAnswersWithUtr: UserAnswers =
    testExistingUserAnswers.set(UniqueTaxpayerReferenceInUserAnswers, testUtr).success.value

  "Change Contact Details Index Controller" - {
    "when the service call to the get the display subscription details returns none" - {
      "must redirect to the journey recovery page" in new Setup {
        when(mockSubscriptionService.displaySubscription(any())(any(), any())).thenReturn(Future.successful(None))

        val request = FakeRequest(GET, routes.ChangeContactDetailsIndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
        verify(mockSubscriptionService, times(1)).displaySubscription(any())(any(), any())
      }
    }

    "when display subscription response is organisation" - {
      "must redirect user to the placeholder controller" in new Setup {
        when(mockSubscriptionService.displaySubscription(any())(any(), any()))
          .thenReturn(Future.successful(Some(testOrganisationDisplaySubscriptionResponse)))

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.ChangeContactDetailsIndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.PlaceholderController
            .onPageLoad(
              "Should redirect to /change-contact/organisation/details (CARF-141)"
            )
            .url
        )
        verify(mockSubscriptionService, times(1)).displaySubscription(any())(any(), any())
        verify(mockSessionRepository)
          .set(
            argThat(_.changeIsIndividualRegType.get == false)
          )
      }
    }

    "when display subscription response is of type none" - {
      "must redirect user to journey recovery" in new Setup {
        when(mockSubscriptionService.displaySubscription(any())(any(), any()))
          .thenReturn(Future.successful(Some(testInvalidDisplaySubscriptionResponse)))

        val request = FakeRequest(GET, routes.ChangeContactDetailsIndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)
        verify(mockSubscriptionService, times(1)).displaySubscription(any())(any(), any())
      }
    }

    "when display subscription response is individual" - {
      "must set user answers with all page info and redirect successfully when phone is none" in new Setup {
        when(mockSubscriptionService.displaySubscription(any())(any(), any()))
          .thenReturn(Future.successful(Some(testIndividualDisplaySubscriptionResponse(hasPhone = false))))
        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.ChangeContactDetailsIndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ChangeIndividualContactDetailsController.onPageLoad().url
        )
        verify(mockSessionRepository, times(1)).set(
          argThat(ua =>
            ua.get(ChangeDetailsIndividualEmailPage).contains(testEmail) &&
              ua.get(ChangeDetailsIndividualPhoneNumberPage).isEmpty
          )
        )

        verify(mockSessionRepository)
          .set(
            argThat(_.changeIsIndividualRegType.get == true)
          )
      }
      "must set user answers with all page info and redirect successfully when phone is returned from the service" in new Setup {
        when(mockSubscriptionService.displaySubscription(any())(any(), any()))
          .thenReturn(Future.successful(Some(testIndividualDisplaySubscriptionResponse(hasPhone = true))))

        when(mockSessionRepository.set(any())).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.ChangeContactDetailsIndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ChangeIndividualContactDetailsController.onPageLoad().url
        )
        verify(mockSessionRepository, times(1)).set(
          argThat(ua =>
            ua.get(ChangeDetailsIndividualEmailPage).contains(testEmail) &&
              ua.get(ChangeDetailsIndividualPhoneNumberPage).contains(testPhone)
          )
        )

        verify(mockSessionRepository)
          .set(
            argThat(_.changeIsIndividualRegType.get == true)
          )
      }
    }
  }

  class Setup {
    val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]
    val application: Application                     = applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(bind[SubscriptionService].toInstance(mockSubscriptionService))
      .build()
  }
}
