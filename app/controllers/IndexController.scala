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

import models.JourneyType.OrgWithUtr
import controllers.actions.{CheckEnrolledToServiceAction, CtUtrRetrievalAction, DataRetrievalAction, IdentifierAction, SubmissionLockAction}
import models.{NormalMode, UserAnswers}
import pages.organisation.UniqueTaxpayerReferenceInUserAnswers
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndexController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    checkEnrolment: CheckEnrolledToServiceAction,
    retrieveCtUTR: CtUtrRetrievalAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify() andThen checkEnrolment andThen retrieveCtUTR() andThen getData() andThen submissionLock()).async {
      implicit request =>
        request.affinityGroup match {
          case AffinityGroup.Individual =>
            for {
              _ <- sessionRepository.set(request.userAnswers.getOrElse(UserAnswers(id = request.userId)))
            } yield Redirect(controllers.individual.routes.IndividualRegistrationTypeController.onPageLoad(NormalMode))

          case _ =>
            request.utr match {
              case Some(utr) =>
                for {
                  autoMatchedUserAnswers <-
                    Future.fromTry(
                      request.userAnswers
                        .getOrElse(
                          UserAnswers(id = request.userId, isCtAutoMatched = true, journeyType = Some(OrgWithUtr))
                        )
                        .set(UniqueTaxpayerReferenceInUserAnswers, utr)
                    )
                  _                      <- sessionRepository.set(autoMatchedUserAnswers)
                } yield Redirect(controllers.routes.IsThisYourBusinessController.onPageLoad(NormalMode))
              case None      =>
                for {
                  _ <- sessionRepository.set(request.userAnswers.getOrElse(UserAnswers(id = request.userId)))
                } yield Redirect(
                  controllers.organisation.routes.OrganisationRegistrationTypeController.onPageLoad(NormalMode)
                )

            }
        }
    }
}
