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
import models.JourneyType.*
import pages.*
import pages.organisation.{FirstContactEmailPage, OrganisationSecondContactEmailPage, UniqueTaxpayerReferenceInUserAnswers}
import pages.individual.NiNumberPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.EmailService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RegistrationConfirmationView

import scala.concurrent.{ExecutionContext, Future}

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

  def onPageLoad(): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>

      // TODO replace with real subscription ID
      val subscriptionIdOpt = Some("XXCAR0012345678")
      val primaryEmailOpt   = request.userAnswers.get(FirstContactEmailPage)
      val journeyTypeOpt    = request.userAnswers.journeyType

      val idNumberOpt: Option[String] = journeyTypeOpt match {
        case Some(OrgWithUtr) | Some(IndWithUtr)     =>
          request.userAnswers.get(UniqueTaxpayerReferenceInUserAnswers).map(_.uniqueTaxPayerReference)
        case Some(IndWithNino)                       =>
          request.userAnswers.get(NiNumberPage)
        case Some(OrgWithoutId) | Some(IndWithoutId) =>
          None
        case _                                       =>
          None
      }

      (subscriptionIdOpt, primaryEmailOpt) match {
        case (Some(subscriptionId), Some(primaryEmail)) =>
          val secondaryEmailOpt = request.userAnswers.get(OrganisationSecondContactEmailPage)

          val addProviderUrl = journeyTypeOpt match {
            case Some(OrgWithUtr) | Some(OrgWithoutId) =>
              if (request.userAnswers.isCtAutoMatched)
                controllers.routes.PlaceholderController
                  .onPageLoad("redirect to /report-for-registered-business (ct automatch)")
                  .url
              else
                controllers.routes.PlaceholderController
                  .onPageLoad("redirect to /organisation-or-individual (non-automatch)")
                  .url

            case Some(IndWithNino) | Some(IndWithUtr) | Some(IndWithoutId) =>
              controllers.routes.PlaceholderController
                .onPageLoad("redirect to /organisation-or-individual (individual)")
                .url

            case _ =>
              controllers.routes.JourneyRecoveryController.onPageLoad().url
          }

          val emailList = primaryEmail :: secondaryEmailOpt.toList

          emailService
            .sendRegistrationConfirmation(emailList, subscriptionId, idNumberOpt)
            .flatMap { _ =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(SubmissionSucceededPage, true))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Ok(
                view(
                  subscriptionId = subscriptionId,
                  primaryEmail = primaryEmail,
                  secondaryEmailOpt = secondaryEmailOpt,
                  addProviderUrl = addProviderUrl,
                  idNumberOpt = idNumberOpt
                )
              )
            }
            .recover { case ex =>
              logger.error("Error processing registration confirmation", ex)
              Redirect(routes.JourneyRecoveryController.onPageLoad())
            }

        case _ =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
