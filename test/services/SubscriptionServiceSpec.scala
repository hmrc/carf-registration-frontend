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

package services

import base.SpecBase
import cats.data.EitherT
import connectors.SubscriptionConnector
import models.*
import models.error.ApiError
import models.error.ApiError.InternalServerError
import models.requests.CreateSubscriptionRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import pages.SafeIdPage
import pages.individual.{IndividualEmailPage, IndividualPhoneNumberPage, WhatIsYourNameIndividualPage}
import utils.SubscriptionHelper

import scala.concurrent.Future

class SubscriptionServiceSpec extends SpecBase {
  val mockConnector: SubscriptionConnector   = mock[SubscriptionConnector]
  val subscriptionHelper: SubscriptionHelper = new SubscriptionHelper()
  val testService                            = new SubscriptionService(mockConnector, subscriptionHelper)

  val exampleSubscriptionId: SubscriptionId = SubscriptionId("XCARF1234567890")
  val exampleSafeId: SafeId                 = SafeId("XE0000123456789")

  val userAnswers: UserAnswers = emptyUserAnswers
    .copy(journeyType = Some(JourneyType.IndWithNino))
    .set(SafeIdPage, exampleSafeId)
    .success
    .value
    .set(WhatIsYourNameIndividualPage, Name("John", "Doe"))
    .success
    .value
    .set(IndividualEmailPage, "john.doe@example.com")
    .success
    .value
    .set(IndividualPhoneNumberPage, "01234567890")
    .success
    .value

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "SubscriptionService" - {
    "subscribe" - {
      "should successfully create subscription with valid user answers" in {
        when(mockConnector.createSubscription(any[CreateSubscriptionRequest])(any(), any())).thenReturn(
          EitherT.rightT[Future, ApiError](exampleSubscriptionId)
        )

        val result = testService.subscribe(userAnswers).futureValue

        result mustBe Right(exampleSubscriptionId)
        verify(mockConnector).createSubscription(any[CreateSubscriptionRequest])(any(), any())
      }

      "should return error when connector fails" in {
        when(mockConnector.createSubscription(any[CreateSubscriptionRequest])(any(), any())).thenReturn(
          EitherT.leftT[Future, SubscriptionId](InternalServerError)
        )

        val result = testService.subscribe(userAnswers).futureValue

        result mustBe Left(InternalServerError)
      }

      "should return BadRequestError when there is a problem with userAnswers" in {
        val result = testService.subscribe(emptyUserAnswers).futureValue

        result mustBe Left(ApiError.BadRequestError)
        verify(mockConnector, never()).createSubscription(any[CreateSubscriptionRequest])(any(), any())
      }
    }
  }
}
