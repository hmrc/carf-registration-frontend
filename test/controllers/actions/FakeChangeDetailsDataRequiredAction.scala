package controllers.actions

import controllers.routes
import models.{DataRequestWithSubscriptionId, IdentifierRequestWithSubscriptionId, UserAnswers}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import scala.concurrent.{ExecutionContext, Future}

class FakeChangeDetailsDataRequiredAction(maybeUserAnswers: Option[UserAnswers])
    extends ActionRefiner[IdentifierRequestWithSubscriptionId, DataRequestWithSubscriptionId] with ChangeDetailsDataRequiredAction {

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
