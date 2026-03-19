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
import models.{JourneyType, UserAnswers}
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

import javax.inject.Inject
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
      request.userAnswers.get(SubmissionSucceededPage) match {

        case Some(true) =>
          (request.userAnswers.subscriptionId, request.userAnswers.journeyType) match {
            case (Some(subscriptionId), Some(journeyType)) =>
              val contactsOpt    = getContacts(journeyType, request.userAnswers)
              val contacts       = contactsOpt.getOrElse(Nil)
              val addProviderUrl = getAddProviderUrl(journeyType, request.userAnswers.isCtAutoMatched)
              val emailAddresses = contacts.map(_.email)

              Future.successful(
                Ok(
                  view(
                    subscriptionId = subscriptionId.value,
                    emailAddresses = emailAddresses,
                    addProviderUrl = addProviderUrl
                  )
                )
              )

            case _ =>
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
          }

        case _ =>
          val resultOpt = for {
            subscriptionId <- request.userAnswers.subscriptionId
            journeyType    <- request.userAnswers.journeyType
            contacts       <- getContacts(journeyType, request.userAnswers)
          } yield {
            val addProviderUrl = getAddProviderUrl(journeyType, request.userAnswers.isCtAutoMatched)
            val emailAddresses = contacts.map(_.email)

            for {
              _              <- emailService.sendRegistrationConfirmation(contacts, subscriptionId.value)
              _               = logger.info("[RegistrationConfirmationController] Email(s) sent successfully.")
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SubmissionSucceededPage, true))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Ok(
              view(
                subscriptionId = subscriptionId.value,
                emailAddresses = emailAddresses,
                addProviderUrl = addProviderUrl
              )
            )
          }

          resultOpt match {
            case Some(successF) =>
              successF.recover { case ex =>
                logger.error("[RegistrationConfirmationController] Error sending confirmation emails", ex)
                Redirect(routes.JourneyRecoveryController.onPageLoad().url)
              }
            case None           =>
              Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad().url))
          }
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
            hasSecond <- userAnswers.get(OrganisationHaveSecondContactPage)
            if hasSecond
            n         <- userAnswers.get(OrganisationSecondContactNamePage)
            e         <- userAnswers.get(OrganisationSecondContactEmailPage)
          } yield ContactEmailInfo(n, e)

          secondContactOpt match {
            case Some(second) => List(firstContact, second)
            case None         => List(firstContact)
          }
        }

      case IndWithNino | IndWithUtr | IndWithoutId =>
        val nameOpt = journeyType match {
          case IndWithoutId             => userAnswers.get(IndWithoutNinoNamePage)
          case IndWithNino | IndWithUtr => userAnswers.get(WhatIsYourNameIndividualPage)
        }
        for {
          name  <- nameOpt
          email <- userAnswers.get(IndividualEmailPage)
        } yield List(ContactEmailInfo(s"${name.firstName} ${name.lastName}", email))
    }

  private def getAddProviderUrl(journeyType: JourneyType, isCtAutoMatched: Boolean): String =
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
}
