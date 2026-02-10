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
import models.JourneyType
import models.JourneyType.{IndWithNino, OrgWithUtr}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CheckYourAnswersHelper
import viewmodels.Section
import views.html.CheckYourAnswersView

import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    helper: CheckYourAnswersHelper,
    subscriptionService: SubscriptionService,
    view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData) {
    implicit request =>

      val journeyType: Option[JourneyType] = request.userAnswers.journeyType

      val businessDetailsSectionMaybe: Option[Section]      =
        helper.getBusinessDetailsSectionMaybe(request.userAnswers)
      val firstContactDetailsSectionMaybe: Option[Section]  =
        helper.getFirstContactDetailsSectionMaybe(request.userAnswers)
      val secondContactDetailsSectionMaybe: Option[Section] =
        helper.getSecondContactDetailsSectionMaybe(request.userAnswers)

      val indWithNinoYourDetails: Option[Section] =
        helper.indWithNinoYourDetailsMaybe(request.userAnswers)
      val indContactDetails: Option[Section]      =
        helper.indContactDetailsMaybe(request.userAnswers)

      val sectionsMaybe = journeyType match {
        case Some(OrgWithUtr)  =>
          for {
            section1 <- businessDetailsSectionMaybe
            section2 <- firstContactDetailsSectionMaybe
            section3 <- secondContactDetailsSectionMaybe
          } yield Seq(section1, section2, section3)
        case Some(IndWithNino) =>
          for {
            section1 <- indWithNinoYourDetails
            section2 <- indContactDetails
          } yield Seq(section1, section2)
        case _                 => None
      }

      sectionsMaybe match {
        case Some(sections: Seq[Section]) => Ok(view(sections))
        case None                         => Redirect(controllers.routes.InformationMissingController.onPageLoad())
      }
  }

  def onSubmit(): Action[AnyContent] = (identify() andThen getData() andThen requireData).async { implicit request =>
    subscriptionService.subscribe(request.userAnswers) map {
      case Right(response) =>
        Redirect(
          controllers.routes.RegistrationConfirmationController.onPageLoad()
        )
      case Left(error)     => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  }
}
