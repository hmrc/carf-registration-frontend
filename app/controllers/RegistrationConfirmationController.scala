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
import models.{JourneyType, UserAnswers}

import javax.inject.Inject
import models.JourneyType.*
import pages.*
import pages.organisation.{FirstContactEmailPage, OrganisationHaveSecondContactPage, OrganisationSecondContactEmailPage, UniqueTaxpayerReferenceInUserAnswers}
import pages.individual.NiNumberPage
import pages.individual.IndividualEmailPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
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

  private def getEmailAddresses(
      journeyType: JourneyType,
      userAnswers: UserAnswers
  ): Option[List[String]] =
    journeyType match {
      case OrgWithUtr | OrgWithoutId =>
        userAnswers
          .get(FirstContactEmailPage)
          .flatMap { firstEmail =>
            userAnswers.get(OrganisationHaveSecondContactPage).flatMap {
              case true  =>
                userAnswers.get(OrganisationSecondContactEmailPage).map(second => List(firstEmail, second))
              case false =>
                Some(List(firstEmail))
            }
          }

      case IndWithNino | IndWithUtr | IndWithoutId =>
        userAnswers.get(IndividualEmailPage).map(List(_))
    }

  private def getIdNumber(
      journeyType: JourneyType,
      userAnswers: UserAnswers
  ): Option[String] =
    journeyType match {
      case OrgWithUtr | IndWithUtr     =>
        userAnswers.get(UniqueTaxpayerReferenceInUserAnswers).map(_.uniqueTaxPayerReference)
      case IndWithNino                 =>
        userAnswers.get(NiNumberPage)
      case OrgWithoutId | IndWithoutId =>
        None
    }

  private def getAddProviderUrl(
      journeyType: JourneyType,
      isCtAutoMatched: Boolean
  ): String =
    journeyType match {
      case OrgWithUtr | OrgWithoutId if isCtAutoMatched =>
        controllers.routes.PlaceholderController
          .onPageLoad("redirect to /report-for-registered-business (ct automatch) (CARF-368)")
          .url
      case OrgWithUtr | OrgWithoutId                    =>
        controllers.routes.PlaceholderController
          .onPageLoad("redirect to /organisation-or-individual (non-automatch) (CARF-368)")
          .url
      case IndWithNino | IndWithUtr | IndWithoutId      =>
        controllers.routes.PlaceholderController
          .onPageLoad("redirect to /organisation-or-individual (individual) (CARF-368)")
          .url
    }

  def onPageLoad(): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>

      val result = for {
        // TODO CARF-325: replace with real subscription ID
        subscriptionId <- Some("XXCAR0012345678")
        journeyType    <- request.userAnswers.journeyType
        emailAddresses <- getEmailAddresses(journeyType, request.userAnswers)
        idNumberOpt     = getIdNumber(journeyType, request.userAnswers)
        addProviderUrl  = getAddProviderUrl(journeyType, request.userAnswers.isCtAutoMatched)
      } yield for {
        _              <- emailService.sendRegistrationConfirmation(emailAddresses, subscriptionId, idNumberOpt)
        updatedAnswers <- Future.fromTry(request.userAnswers.set(SubmissionSucceededPage, true))
        _              <- sessionRepository.set(updatedAnswers)
      } yield Ok(
        view(
          subscriptionId = subscriptionId,
          emailAddresses = emailAddresses,
          addProviderUrl = addProviderUrl
        )
      )
      result match {
        case Some(successRedirect) => successRedirect
        case None                  =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
      }
    }
}
