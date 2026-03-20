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

package controllers.actions

import base.SpecBase
import controllers.routes
import models.{DataRequestWithSubscriptionId, IdentifierRequestWithSubscriptionId}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.LOCATION
import repositories.SessionRepository
import services.SubscriptionService

import scala.concurrent.Future

class ChangeDetailsDataRequiredActionSpec extends SpecBase {

  private val mockSessionRepository   = mock[SessionRepository]
  private val mockSubscriptionService = mock[SubscriptionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionRepository, mockSubscriptionService)
  }

  class Harness(sessionRepository: SessionRepository, subscriptionService: SubscriptionService)
      extends ChangeDetailsDataRequiredActionImpl(sessionRepository, subscriptionService) {
    def actionRefine[A](
        request: IdentifierRequestWithSubscriptionId[A]
    ): Future[Either[Result, DataRequestWithSubscriptionId[A]]] = refine(request)
  }

  val request: IdentifierRequestWithSubscriptionId[AnyContentAsEmpty.type] =
    IdentifierRequestWithSubscriptionId(FakeRequest(), testInternalId, testSubscriptionId)

  "ChangeDetailsDataRequiredAction" - {

    "must redirect to the Journey Recovery when User Answers is None" in {
      val harness = new Harness(mockSessionRepository, mockSubscriptionService)

      when(mockSessionRepository.get(any())).thenReturn(Future.successful(None))

      val result =
        harness
          .actionRefine(request)
          .futureValue
          .left
          .getOrElse(
            fail()
          )
          .header

      result.status                mustBe SEE_OTHER
      result.headers.get(LOCATION) mustBe Some(routes.JourneyRecoveryController.onPageLoad().url)

      verify(mockSubscriptionService, times(0)).displaySubscription(any())(any(), any())
    }

    "must return a DataRequestWithSubscriptionId with the correct values when User Answers exists with displaySubscriptionResponse" in {
      val harness                                    = new Harness(mockSessionRepository, mockSubscriptionService)
      val userAnswersWithDisplaySubscriptionResponse =
        emptyUserAnswers.copy(displaySubscriptionResponse =
          Some(testIndividualDisplaySubscriptionResponse(hasPhone = true))
        )

      when(mockSessionRepository.get(any())).thenReturn(
        Future.successful(
          Some(userAnswersWithDisplaySubscriptionResponse)
        )
      )

      val result =
        harness
          .actionRefine(request)
          .futureValue
          .toOption
          .getOrElse(
            fail()
          )

      result mustBe DataRequestWithSubscriptionId(
        request = request.request,
        userId = request.userId,
        subscriptionId = request.subscriptionId,
        userAnswers = userAnswersWithDisplaySubscriptionResponse
      )

      verify(mockSubscriptionService, times(0)).displaySubscription(any())(any(), any())
    }

    "must return a DataRequestWithSubscriptionId with displaySubscriptionResponse when it isn't in the original user answers" in {
      val harness                                    = new Harness(mockSessionRepository, mockSubscriptionService)
      val userAnswersWithDisplaySubscriptionResponse =
        emptyUserAnswers.copy(displaySubscriptionResponse =
          Some(testIndividualDisplaySubscriptionResponse(hasPhone = true))
        )

      when(mockSessionRepository.get(any())).thenReturn(
        Future.successful(
          Some(emptyUserAnswers)
        )
      )

      when(mockSubscriptionService.displaySubscription(any())(any(), any())).thenReturn(
        Future.successful(
          Some(testIndividualDisplaySubscriptionResponse(hasPhone = true))
        )
      )

      val result =
        harness
          .actionRefine(request)
          .futureValue
          .toOption
          .getOrElse(
            fail()
          )

      result mustBe DataRequestWithSubscriptionId(
        request = request.request,
        userId = request.userId,
        subscriptionId = request.subscriptionId,
        userAnswers = userAnswersWithDisplaySubscriptionResponse
      )

      verify(mockSubscriptionService, times(1)).displaySubscription(any())(any(), any())
    }
  }

}
