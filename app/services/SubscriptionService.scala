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

import connectors.SubscriptionConnector
import models.error.ApiError
import models.requests.CreateSubscriptionRequest
import models.{SubscriptionId, UserAnswers}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import utils.SubscriptionHelper

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionService @Inject() (
    subscriptionConnector: SubscriptionConnector,
    subscriptionHelper: SubscriptionHelper
) extends Logging {

  def subscribe(userAnswers: UserAnswers)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[Either[ApiError, SubscriptionId]] = {
    val maybeRequest: Option[CreateSubscriptionRequest] = subscriptionHelper.buildSubscriptionRequest(userAnswers)

    maybeRequest match {
      case Some(request) =>
        subscriptionConnector
          .createSubscription(request)
          .value
          .map {
            case Right(result) => Right(result)
            case Left(error)   =>
              logger.error(s"Failed to create subscription: $error")
              Left(error)
          }
      case None          =>
        logger.error("There has been an error building the subscription request from user answers")
        Future.successful(Left(ApiError.BadRequestError))
    }
  }
}
