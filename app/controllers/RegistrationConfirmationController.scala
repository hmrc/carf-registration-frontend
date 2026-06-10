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
import models.JourneyType.*
import models.{JourneyType, RegistrationType, SubscriptionId, UserAnswers}
import pages.*
import pages.individual.{IndividualEmailPage, WhatIsYourNameIndividualPage}
import pages.individualWithoutId.IndWithoutNinoNamePage
import pages.organisation.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{ContactEmailInfo, EmailService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RegistrationConfirmationView
import config.FrontendAppConfig

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationConfirmationController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    appConfig: FrontendAppConfig,
    emailService: EmailService,
    val controllerComponents: MessagesControllerComponents,
    view: RegistrationConfirmationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify(redirect = false) andThen getData() andThen requireData).async { implicit request =>

      val resultOpt = for {
        subscriptionId <- request.userAnswers.subscriptionId
        journeyType    <- request.userAnswers.journeyType
        contacts       <- getContacts(journeyType, request.userAnswers)
      } yield {
        val addProviderUrl = getAddProviderUrl
        logger.info(s"addProviderUrl = $addProviderUrl")

        val haveEmailsSentAlready: Boolean = request.userAnswers.get(SubmissionSucceededPage).getOrElse(false)
        val maybeSubscriptionId            =
          journeyType match {
            case OrgWithUtr | OrgWithoutId => Some(subscriptionId.value)
            case _                         => None
          }
        for {
          _              <- emailService.sendEmails(contacts, maybeSubscriptionId, haveEmailsSentAlready)
          updatedAnswers <- Future.fromTry(request.userAnswers.set(SubmissionSucceededPage, true))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Ok(
          view(
            subscriptionId = subscriptionId.value,
            emailAddresses = contacts.map(_.email),
            addProviderUrl = addProviderUrl
          )
        )
      }

      resultOpt match {
        case Some(result) => result
        case None         =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
      }
    }

  private def getContacts(journeyType: JourneyType, userAnswers: UserAnswers): Option[List[ContactEmailInfo]] =
    journeyType match {
      case OrgWithUtr | OrgWithoutId =>
        for {
          firstName  <- userAnswers.get(FirstContactNamePage)
          firstEmail <- userAnswers.get(FirstContactEmailPage)
        } yield {
          val firstContact = ContactEmailInfo(firstName, firstEmail)

          val secondContactOpt = for {
            hasSecond   <- userAnswers.get(OrganisationHaveSecondContactPage)
            if hasSecond
            secondName  <- userAnswers.get(OrganisationSecondContactNamePage)
            secondEmail <- userAnswers.get(OrganisationSecondContactEmailPage)
          } yield ContactEmailInfo(secondName, secondEmail)

          List(firstContact) ++ secondContactOpt
        }

      case IndWithNino | IndWithUtr | IndWithoutId =>
        val nameOpt = journeyType match {
          case IndWithoutId => userAnswers.get(IndWithoutNinoNamePage)
          case IndWithNino  => userAnswers.get(WhatIsYourNameIndividualPage)
          case _            => userAnswers.get(WhatIsYourNamePage)
        }

        for {
          name  <- nameOpt
          email <- userAnswers.get(IndividualEmailPage)
        } yield List(ContactEmailInfo(name.fullName, email))
    }

  private def getAddProviderUrl: String = appConfig.managementRoutingUrl
}
