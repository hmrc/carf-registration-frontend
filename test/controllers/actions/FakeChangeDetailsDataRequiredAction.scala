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

package controllers.actions

import controllers.routes
import models.{DataRequestWithSubscriptionId, IdentifierRequestWithSubscriptionId, UserAnswers}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import scala.concurrent.{ExecutionContext, Future}

class FakeChangeDetailsDataRequiredAction(maybeUserAnswers: Option[UserAnswers])
    extends ActionRefiner[IdentifierRequestWithSubscriptionId, DataRequestWithSubscriptionId]
    with ChangeDetailsDataRequiredAction {

  override protected def refine[A](
      request: IdentifierRequestWithSubscriptionId[A]
  ): Future[Either[Result, DataRequestWithSubscriptionId[A]]] =
    maybeUserAnswers match {
      case Some(userAnswers) =>
        Future.successful(
          Right(DataRequestWithSubscriptionId(request.request, request.userId, request.subscriptionId, userAnswers))
        )
      case None              => Future.successful(Left(Redirect(routes.JourneyRecoveryController.onPageLoad())))

    }

  implicit override protected val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
