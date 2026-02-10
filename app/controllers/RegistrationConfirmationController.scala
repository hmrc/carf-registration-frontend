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
import models.UserAnswers
import pages.*
import pages.organisation.{FirstContactEmailPage, OrganisationSecondContactEmailPage, UniqueTaxpayerReferenceInUserAnswers}
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
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    emailService: EmailService,
    val controllerComponents: MessagesControllerComponents,
    view: RegistrationConfirmationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>

      // TODO add real subscirption id in future ticket
      val subscriptionIdOpt = Some("XXCAR0012345678")
      val primaryEmailOpt   = request.userAnswers.get(FirstContactEmailPage)
      val idNumberOpt       = request.userAnswers.get(UniqueTaxpayerReferenceInUserAnswers).map(_.uniqueTaxPayerReference)

      (subscriptionIdOpt, primaryEmailOpt, idNumberOpt) match {
        case (Some(subscriptionId), Some(primaryEmail), Some(idNumber)) =>
          val secondaryEmailOpt = request.userAnswers.get(OrganisationSecondContactEmailPage)

          val wasCtAutomatched = request.userAnswers.isCtAutoMatched

          val addProviderUrl = if (wasCtAutomatched) {
            controllers.routes.ReportForRegisteredBusinessController.onPageLoad().url
          } else {
            controllers.routes.OrganisationOrIndividualController.onPageLoad().url
          }

          val emailList = secondaryEmailOpt match {
            case Some(secondaryEmail) => List(primaryEmail, secondaryEmail)
            case None                 => List(primaryEmail)
          }

          emailService.sendRegistrationConfirmation(emailList, subscriptionId, idNumber).flatMap { _ =>

            val updatedAnswers = request.userAnswers.set(SubmissionSucceededPage, true)

            updatedAnswers match {
              case Success(answers) =>
                // Persist the updated answers
                sessionRepository
                  .set(answers)
                  .map { _ =>
                    Ok(
                      view(
                        subscriptionId = subscriptionId,
                        primaryEmail = primaryEmail,
                        secondaryEmailOpt = secondaryEmailOpt,
                        addProviderUrl = addProviderUrl
                      )
                    )
                  }
                  .recover { case _ =>
                    Redirect(routes.JourneyRecoveryController.onPageLoad())
                  }
              case Failure(_)       =>
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            }
          }

        case _ =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

}
