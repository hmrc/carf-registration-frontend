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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.routes
import models.{IdentifierRequestWithSubscriptionId, IdentifierType, SubscriptionId}
import play.api.Logging
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait CarfIdRetrievalAction {

  def apply(): ActionBuilder[IdentifierRequestWithSubscriptionId, AnyContent]
    with ActionFunction[Request, IdentifierRequestWithSubscriptionId]

}

class CarfIdRetrievalActionImpl @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends CarfIdRetrievalAction
    with AuthorisedFunctions {

  override def apply(): ActionBuilder[IdentifierRequestWithSubscriptionId, AnyContent]
    with ActionFunction[Request, IdentifierRequestWithSubscriptionId] =
    new CarfIdRetrievalActionExtractor(authConnector, config, parser)

}

class CarfIdRetrievalActionExtractor @Inject() (
    override val authConnector: AuthConnector,
    config: FrontendAppConfig,
    val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends ActionBuilder[IdentifierRequestWithSubscriptionId, AnyContent]
    with ActionFunction[Request, IdentifierRequestWithSubscriptionId]
    with AuthorisedFunctions
    with Logging {

  override def invokeBlock[A](
      request: Request[A],
      block: IdentifierRequestWithSubscriptionId[A] => Future[Result]
  ): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.internalId and Retrievals.allEnrolments) {
      case Some(internalId) ~ enrolments =>
        getCarfId(enrolments) match {
          case Some(subscriptionId) =>
            block(IdentifierRequestWithSubscriptionId(request, internalId, SubscriptionId(subscriptionId)))
          case None                 =>
            logger.info("User has no CARF enrolment. Taking user to the start of the registration journey.")
            Future.successful(Redirect(controllers.routes.IndexController.onPageLoad()))
        }
      case _                             =>
        val msg = "Unable to retrieve internal id"
        logger.warn(msg)
        throw AuthorisationException.fromString(msg)
    } recover {
      case _: NoActiveSession        =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }

  private def getCarfId(
      enrolments: Enrolments
  ): Option[String] =
    for {
      enrolment <- enrolments.getEnrolment(config.enrolmentKey)
      id        <- enrolment.getIdentifier(IdentifierType.CARFID)
      carfId    <- if (id.value.nonEmpty) Some(id.value) else None
    } yield carfId

}
