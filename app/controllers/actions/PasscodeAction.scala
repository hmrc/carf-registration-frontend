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

import config.FrontendAppConfig
import controllers.routes
import models.requests.PasscodeRequest
import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PasscodeActionImpl @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends PasscodeAction
    with AuthorisedFunctions {

  override def apply(
      redirect: Boolean = true
  ): ActionBuilder[PasscodeRequest, AnyContent] with ActionFunction[Request, PasscodeRequest] =
    new PasscodeActionExtractor(authConnector, config, parser)
}

class PasscodeActionExtractor @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[PasscodeRequest, AnyContent]
    with ActionFunction[Request, PasscodeRequest]
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: PasscodeRequest[A] => Future[Result]): Future[Result] = {
    implicit val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    authorised()
      .retrieve(Retrievals.internalId) {
        case Some(id) => block(PasscodeRequest(request = request, userId = id))
        case _        => Future.successful(Redirect(routes.PlaceholderController.onPageLoad("Could not retrieve id!")))
      } recover { case _: NoActiveSession =>
      Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
    }
  }

}

trait PasscodeAction {
  def apply(
      redirect: Boolean = true
  ): ActionBuilder[PasscodeRequest, AnyContent] with ActionFunction[Request, PasscodeRequest]
}
