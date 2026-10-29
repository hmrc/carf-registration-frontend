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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.requests.IdentifierRequest
import pages.PasscodePage
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.*
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentialRole, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.LoggerUtil.logWarn

import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction {
  def apply(
      redirect: Boolean = true
  ): ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest]
}

class AuthenticatedIdentifierAction @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default,
    sessionRepository: SessionRepository
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions {

  override def apply(
      redirect: Boolean = true
  ): ActionBuilder[IdentifierRequest, AnyContent] with ActionFunction[Request, IdentifierRequest] =
    new AuthenticatedIdentifierActionWithRegime(authConnector, config, parser, redirect, sessionRepository)
}

class AuthenticatedIdentifierActionWithRegime @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default,
    val redirect: Boolean,
    sessionRepository: SessionRepository
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]
    with AuthorisedFunctions
    with Logging {

  private def enrolmentKey: String = config.enrolmentKey

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(
      internalId and allEnrolments and affinityGroup and credentialRole
    ) {
      case _ ~ _ ~ Some(Agent) ~ _                                                                   =>
        Future.successful(Redirect(routes.AgentSignInProblemController.onPageLoad()))
      case _ ~ enrolments ~ _ ~ _ if enrolments.enrolments.exists(_.key == enrolmentKey) && redirect =>
        Future.successful(Redirect(config.carfManagementFrontendHomePageUrl))
      case _ ~ _ ~ _ ~ Some(Assistant)                                                               =>
        Future.successful(Redirect(routes.AssistantSignInProblemController.onPageLoad()))
      case Some(internalID) ~ enrolments ~ Some(affinityGroup) ~ _                                   =>
        handlePasscodeCheck(internalID).flatMap {
          case true => block(IdentifierRequest(request, internalID, affinityGroup, enrolments.enrolments))
          case _    => Future.successful(Redirect(routes.PlaceholderController.onPageLoad("Content not yet available")))
        }

      case _ =>
        logWarn("[AuthenticatedIdentifierActionWithRegime][invokeBlock] Failed to retrieve valid auth data")
        throw new UnauthorizedException("Failed to retrieve valid auth data")
    } recover {
      case _: NoActiveSession        =>
        logWarn("[AuthenticatedIdentifierActionWithRegime][invokeBlock] No active session found")
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        logWarn(
          "[AuthenticatedIdentifierActionWithRegime][invokeBlock] User is not authorised to navigate to this page"
        )
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }

  private def handlePasscodeCheck(userId: String): Future[Boolean] =
    if (config.passcodeCheckEnabled)
      passcodeCheck(userId)
    else
      Future.successful(true)

  private def passcodeCheck(userId: String): Future[Boolean] =
    sessionRepository.get(userId).map(_.exists(_.get(PasscodePage).contains(config.passcode)))
}
