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

import com.google.inject.Inject
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction, SubmissionLockAction}
import models.JourneyType.{IndWithNino, IndWithUtr, IndWithoutId, OrgWithUtr, OrgWithoutId}
import models.error.ApiError
import models.{JourneyType, UserAnswers}
import pages.SubscriptionIdPage
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckYourAnswersHelper
import viewmodels.Section
import views.html.CheckYourAnswersView

import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckYourAnswersHelper,
    submissionLock: SubmissionLockAction,
    subscriptionService: SubscriptionService,
    view: CheckYourAnswersView,
    sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with Logging
    with I18nSupport {

  private def businessDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.getBusinessDetailsSectionMaybe(userAnswers)

  private def orgWithoutIdDetailsMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.getOrgWithoutIdDetailsMaybe(userAnswers)

  private def firstContactDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.getFirstContactDetailsSectionMaybe(userAnswers)

  private def secondContactDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.getSecondContactDetailsSectionMaybe(userAnswers)

  private def indWithNinoYourDetails(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.indWithNinoYourDetailsMaybe(userAnswers)

  private def indWithoutIdYourDetails(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.indWithoutIdYourDetailsMaybe(userAnswers)

  private def indContactDetails(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    helper.indContactDetailsMaybe(userAnswers)

  def onPageLoad(): Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData) {
    implicit request =>
      val userAnswers                      = request.userAnswers
      val journeyType: Option[JourneyType] = userAnswers.journeyType

      val sectionsMaybe = journeyType match {
        case Some(OrgWithUtr)   =>
          for {
            section1 <- businessDetailsSectionMaybe(userAnswers)
            section2 <- firstContactDetailsSectionMaybe(userAnswers)
            section3 <- secondContactDetailsSectionMaybe(userAnswers)
          } yield Seq(section1, section2, section3)
        case Some(IndWithNino)  =>
          for {
            section1 <- indWithNinoYourDetails(userAnswers)
            section2 <- indContactDetails(userAnswers)
          } yield Seq(section1, section2)
        case Some(IndWithUtr)   =>
          for {
            section1 <- businessDetailsSectionMaybe(userAnswers)
            section2 <- indContactDetails(userAnswers)
          } yield Seq(section1, section2)
        case Some(OrgWithoutId) =>
          for {
            section1 <- orgWithoutIdDetailsMaybe(userAnswers)
            section2 <- firstContactDetailsSectionMaybe(userAnswers)
            section3 <- secondContactDetailsSectionMaybe(userAnswers)
          } yield Seq(section1, section2, section3)
        case Some(IndWithoutId) =>
          for {
            section1 <- indWithoutIdYourDetails(userAnswers)
            section2 <- indContactDetails(userAnswers)
          } yield Seq(section1, section2)
        case _                  =>
          logger.warn(s"[CheckYourAnswersController] Error! Journey Type was missing from user answers")
          None
      }

      sectionsMaybe match {
        case Some(sections: Seq[Section]) => Ok(view(sections))
        case None                         => Redirect(controllers.routes.InformationMissingController.onPageLoad())
      }
  }

  def onSubmit(): Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData).async {
    implicit request =>
      subscriptionService.subscribe(request.userAnswers).flatMap {
        case Right(subscriptionId) =>
          request.userAnswers.set(SubscriptionIdPage, subscriptionId).toOption match {
            case Some(updatedAnswers) =>
              sessionRepository.set(updatedAnswers).map { _ =>
                Redirect(controllers.routes.RegistrationConfirmationController.onPageLoad())
              }
            case None                 =>
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        case Left(error)           =>
          logger.error(s"[CheckYourAnswersController] Failed to create subscription: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

}
