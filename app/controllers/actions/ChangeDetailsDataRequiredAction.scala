/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.routes
import models.{DataRequestWithSubscriptionId, IdentifierRequestWithSubscriptionId}
import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import repositories.SessionRepository
import services.SubscriptionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeDetailsDataRequiredActionImpl @Inject() (
    val sessionRepository: SessionRepository,
    val subscriptionService: SubscriptionService
)(implicit val executionContext: ExecutionContext)
    extends ChangeDetailsDataRequiredAction
    with Logging {

  override protected def refine[A](
      request: IdentifierRequestWithSubscriptionId[A]
  ): Future[Either[Result, DataRequestWithSubscriptionId[A]]] =

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    sessionRepository.get(request.userId) flatMap {
      case Some(userAnswers) =>
        if (userAnswers.displaySubscriptionResponse.isEmpty) {
          subscriptionService.displaySubscription(request.subscriptionId) flatMap {
            case Some(value) =>
              Future.successful(
                Right(
                  DataRequestWithSubscriptionId(
                    request.request,
                    request.userId,
                    request.subscriptionId,
                    userAnswers.copy(displaySubscriptionResponse = Some(value))
                  )
                )
              )
            case None        =>
              logger.warn(s"[ChangeDetailsDataRequiredAction] Could not retrieve display subscription details.")
              throw new Exception("Could not retrieve subscription details")
          }
        } else {
          Future.successful(
            Right(
              DataRequestWithSubscriptionId(
                request.request,
                request.userId,
                request.subscriptionId,
                userAnswers
              )
            )
          )
        }
      case None              =>
        logger.warn(s"[ChangeDetailsDataRequiredAction] User answers could not be found for request.")
        Future.successful(Left(Redirect(routes.JourneyRecoveryController.onPageLoad())))
    }

}

trait ChangeDetailsDataRequiredAction
    extends ActionRefiner[IdentifierRequestWithSubscriptionId, DataRequestWithSubscriptionId]
