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
import models.error.ApiError.{InternalServerError, MandatoryInformationMissingError}
import models.error.{ApiError, CarfError}
import models.requests.CreateSubscriptionRequest
import models.responses.*
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
  ): Future[Either[ApiError, SubscriptionId]] =
    subscriptionHelper.buildSubscriptionRequest(userAnswers) match {
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
        logger.error("There has been an error building the subscription request from userAnswers")
        Future.successful(
          Left(
            MandatoryInformationMissingError(
              s"There has been an error building the subscription request from userAnswers"
            )
          )
        )
    }

  def displaySubscription(subscriptionId: SubscriptionId)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[Option[DisplaySubscriptionResponse]] =
    if (subscriptionId.value.take(1) == "9") {
      Future.successful(None)
    } else {
      Future.successful(
        Some(
          DisplaySubscriptionResponse(success =
            DisplaySubscriptionSuccess(
              processingDate = "test-time",
              carfSubscriptionDetails = DisplaySubscriptionDetails(
                tradingName = Some("test-trading-name"),
                gbUser = true,
                primaryContact = primaryContactDetail(subscriptionId),
                secondaryContact = secondaryContactDetail(subscriptionId)
              )
            )
          )
        )
      )
    }

  private def primaryContactDetail(subscriptionId: SubscriptionId): DisplaySubscriptionContact =
    if (subscriptionId.value.take(1) == "1") {
      DisplaySubscriptionContact(
        individual =
          Some(DisplaySubscriptionIndividual(firstName = "Timmy", middleName = Some("John"), lastName = "Jimmy")),
        organisation = None,
        email = "hi@example.com",
        phone = Some("12345"),
        mobile = Some("67890")
      )
    } else {
      DisplaySubscriptionContact(
        individual = None,
        organisation = Some(DisplaySubscriptionOrganisation(name = "ABC Fruit Limited")),
        email = "hi@example.com",
        phone = Some("12345"),
        mobile = Some("67890")
      )
    }

  private def secondaryContactDetail(subscriptionId: SubscriptionId): Option[DisplaySubscriptionContact] =
    if (subscriptionId.value.take(1) == "1") {
      None
    } else if (subscriptionId.value.take(1) == "2") {
      None
    } else {
      Some(
        DisplaySubscriptionContact(
          individual = None,
          organisation = Some(DisplaySubscriptionOrganisation(name = "WindsAndWaves.pkmn")),
          email = "hi@example.com",
          phone = Some("12345"),
          mobile = Some("67890")
        )
      )
    }

  def updateSubscription(subscriptionId: SubscriptionId): Future[Either[CarfError, Unit]] =
    if (subscriptionId.value.take(3) == "199") {
      Future.successful(Left(InternalServerError))
    } else {
      Future.successful(Right((): Unit))
    }
}
