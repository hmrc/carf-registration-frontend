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

package controllers

import controllers.actions.{CheckEnrolledToServiceAction, CtUtrRetrievalAction, DataRetrievalAction, IdentifierAction}
import models.{NormalMode, UserAnswers}
import pages.IndexPage
import play.api.i18n.I18nSupport
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    checkEnrolment: CheckEnrolledToServiceAction,
    retrieveCtUTR: CtUtrRetrievalAction,
    sessionRepository: SessionRepository,
    clock: Clock
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify() andThen checkEnrolment andThen retrieveCtUTR()).async {
    implicit request =>
      request.affinityGroup match {
        case AffinityGroup.Individual =>
          Future.successful(
            Redirect(controllers.routes.IndividualRegistrationTypeController.onPageLoad(NormalMode))
          )
        case _                        =>
          request.utr match {
            case Some(utr) =>
              // org with CT UTR, - CT Automatched - go straight to is this your business?
              val userAnswers = UserAnswers(request.userId, lastUpdated = Instant.now(clock))
              for {
                autoMatchedUserAnswers <- Future.fromTry(userAnswers.set(IndexPage, utr))
                result                 <- sessionRepository.set(autoMatchedUserAnswers).map {
                                            case true  =>
                                              Redirect(controllers.routes.IsThisYourBusinessController.onPageLoad(NormalMode))
                                            case false =>
                                              logger.error(
                                                s"Failed to update user answers with autoMatchedUTR field for userId: [${request.userId}]"
                                              )
                                              Redirect(routes.JourneyRecoveryController.onPageLoad())
                                          }
              } yield result

            case None =>
              // org without CT UTR- manual registration
              Future.successful(
                Redirect(controllers.routes.OrganisationRegistrationTypeController.onPageLoad(NormalMode))
              )
          }
      }
  }
}
