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

package controllers

import controllers.actions.*
import javax.inject.Inject
import models.{Mode, UserAnswers}
import pages.*
import pages.organisation.{FirstContactEmailPage, OrganisationSecondContactEmailPage, YourUniqueTaxpayerReferencePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.EmailService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RegistrationConfirmationView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RegistrationConfirmationController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    emailService: EmailService,
    val controllerComponents: MessagesControllerComponents,
    view: RegistrationConfirmationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      val subscriptionIdOpt = request.userAnswers.get(SubscriptionIdPage)
      val primaryEmailOpt   = request.userAnswers.get(FirstContactEmailPage)

      (subscriptionIdOpt, primaryEmailOpt) match {
        case (Some(subscriptionId), Some(primaryEmail)) =>
          val secondaryEmailOpt = request.userAnswers.get(OrganisationSecondContactEmailPage)

          val wasCtAutomatched = request.userAnswers.get(IndexPage).isDefined

          val addProviderUrl = if (wasCtAutomatched) {
            controllers.routes.ReportForRegisteredBusinessController.onPageLoad().url
          } else {
            controllers.routes.OrganisationOrIndividualController.onPageLoad().url
          }

          val emailList = secondaryEmailOpt match {
            case Some(secondaryEmail) => List(primaryEmail, secondaryEmail)
            case None                 => List(primaryEmail)
          }

          val idNumber = getIdNumber(request.userAnswers)

          emailService.sendRegistrationConfirmation(emailList, subscriptionId.value, idNumber).flatMap { _ =>

            val updatedAnswers = request.userAnswers.set(SubmissionSucceededPage, true)

            updatedAnswers match {
              case Success(answers) =>
                // Persist the updated answers
                sessionRepository.set(answers).map { _ =>
                  Ok(
                    view(
                      subscriptionId = subscriptionId.value,
                      primaryEmail = primaryEmail,
                      secondaryEmailOpt = secondaryEmailOpt,
                      addProviderUrl = addProviderUrl
                    )
                  )
                }
              case Failure(_)       =>
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            }
          }

        case _ =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def getIdNumber(userAnswers: UserAnswers): String =
    (userAnswers.get(IndexPage), userAnswers.get(YourUniqueTaxpayerReferencePage)) match {
      case (Some(indexData), _) => indexData.uniqueTaxPayerReference
      case (_, Some(utrData))   => utrData.uniqueTaxPayerReference
      case (None, None)         =>
        logger.warn("[RegistrationConfirmation] No ID number found in UserAnswers - using default for email stub")
        "1234567890"
    }

}
