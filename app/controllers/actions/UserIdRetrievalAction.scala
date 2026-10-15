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

import com.google.inject.Inject
import controllers.routes
import models.requests.RequestWithOptUserId
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.LoggerUtil.logWarn

import scala.concurrent.{ExecutionContext, Future}

trait UserIdRetrievalAction {
  def apply(): ActionBuilder[RequestWithOptUserId, AnyContent] with ActionFunction[Request, RequestWithOptUserId]
}

class UserIdRetrievalActionImpl @Inject() (
    override val authConnector: AuthConnector,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends UserIdRetrievalAction
    with AuthorisedFunctions {

  override def apply()
      : ActionBuilder[RequestWithOptUserId, AnyContent] with ActionFunction[Request, RequestWithOptUserId] =
    new UserIdRetrievalActionExtractor(authConnector, parser)

}

class UserIdRetrievalActionExtractor @Inject() (
    override val authConnector: AuthConnector,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[RequestWithOptUserId, AnyContent]
    with ActionFunction[Request, RequestWithOptUserId]
    with AuthorisedFunctions {

  override def invokeBlock[A](
      request: Request[A],
      block: RequestWithOptUserId[A] => Future[Result]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.internalId) {
      case Some(internalId) => block(RequestWithOptUserId(request, Some(internalId)))
      case _                =>
        val msg = "[UserIdRetrievalAction] Unable to retrieve internal id"
        logWarn(msg)
        throw AuthorisationException.fromString(msg)
    } recoverWith {
      case _: NoActiveSession        =>
        block(RequestWithOptUserId(request, None))
      case _: AuthorisationException =>
        Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
    }
  }

}
