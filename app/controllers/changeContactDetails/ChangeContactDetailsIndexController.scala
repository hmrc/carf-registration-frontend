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

package controllers.changeContactDetails

import controllers.actions.*
import controllers.routes
import models.responses.{DisplaySubscriptionDetails, DisplaySubscriptionResponse}
import models.{IdentifierRequestWithSubscriptionId, UserAnswers}
import pages.QuestionPage
import pages.changeContactDetails.*
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.SubscriptionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeContactDetailsIndexController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    carfIdRetrieval: CarfIdRetrievalAction,
    sessionRepository: SessionRepository,
    val subscriptionService: SubscriptionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = carfIdRetrieval().async { implicit request =>
    subscriptionService.displaySubscription(request.subscriptionId).value flatMap {
      case Right(subscriptionDetails) =>
        subscriptionDetails.isIndividualRegistrationType match {
          case Some(true)  => processIndividualChangeDetails(subscriptionDetails)
          case Some(false) => processOrganisationChangeDetails(subscriptionDetails)
          case None        =>
            logger.warn(s"[ChangeContactDetailsIndexController] User answers could not be found for request.")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      case _                          =>
        logger.warn(s"[ChangeContactDetailsIndexController] User answers could not be found for request.")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def processIndividualChangeDetails(
      subscriptionDetails: DisplaySubscriptionResponse
  )(implicit request: IdentifierRequestWithSubscriptionId[AnyContent]) =
    for {
      individualDetails <- Future.successful(subscriptionDetails.success.carfSubscriptionDetails.primaryContact)
      a                 <- Future.fromTry(
                             UserAnswers(
                               id = request.userId,
                               changeIsIndividualRegType = Some(true)
                             ).set(ChangeDetailsIndividualEmailPage, individualDetails.email)
                           )
      b                 <- Future.fromTry(a.set(ChangeDetailsIndividualHavePhonePage, individualDetails.phone.nonEmpty))
      updatedAnswers    <- individualDetails.phone match {
                             case Some(phoneNo) =>
                               Future.fromTry(b.set(ChangeDetailsIndividualPhoneNumberPage, phoneNo))
                             case None          => Future.successful(b)
                           }
      _                 <- sessionRepository.set(updatedAnswers)
    } yield Redirect(
      controllers.changeContactDetails.routes.ChangeIndividualContactDetailsController.onPageLoad()
    )

  private def processOrganisationChangeDetails(
      subscriptionDetails: DisplaySubscriptionResponse
  )(implicit request: IdentifierRequestWithSubscriptionId[AnyContent]) = {
    val details                    = subscriptionDetails.success.carfSubscriptionDetails
    val organisationPrimaryDetails = details.primaryContact

    val userAnswers = UserAnswers(
      id = request.userId,
      changeIsIndividualRegType = Some(false)
    )

    for {
      a              <- addOptionalDetailsToUserAnswers(details, userAnswers)
      b              <- Future.fromTry(a.set(ChangeDetailsOrgFirstEmailPage, organisationPrimaryDetails.email))
      c              <- Future.fromTry(b.set(ChangeDetailsOrgFirstHavePhonePage, organisationPrimaryDetails.phone.nonEmpty))
      updatedAnswers <- Future.fromTry(c.set(ChangeDetailsOrgHaveSecondContactPage, details.secondaryContact.isDefined))
      _              <- sessionRepository.set(updatedAnswers)
    } yield Redirect(
      controllers.changeContactDetails.routes.ChangeOrganisationContactDetailsController.onPageLoad()
    )
  }

  private def addOptionalDetailsToUserAnswers(details: DisplaySubscriptionDetails, userAnswers: UserAnswers) = {
    val organisationName               = details.primaryContact.organisation.map(_.name)
    val organisationPhone              = details.primaryContact.phone
    val organisationSecondaryName      = details.secondaryContact.flatMap(_.organisation.map(_.name))
    val organisationSecondaryEmail     = details.secondaryContact.map(_.email)
    val organisationSecondaryHavePhone = details.secondaryContact.map(_.phone.isDefined)
    val organisationSecondaryPhone     = details.secondaryContact.flatMap(_.phone)

    val setUserAnswer: (Option[String], QuestionPage[String], UserAnswers) => Future[UserAnswers] =
      (maybeContactValue, page, ua) =>
        maybeContactValue.fold(Future.successful(ua))(contactValue => Future.fromTry(ua.set(page, contactValue)))

    for {
      a                  <- setUserAnswer(organisationName, ChangeDetailsOrgFirstNamePage, userAnswers)
      b                  <- setUserAnswer(organisationPhone, ChangeDetailsOrgFirstPhoneNumberPage, a)
      c                  <- setUserAnswer(organisationSecondaryName, ChangeDetailsOrgSecondNamePage, b)
      d                  <- setUserAnswer(organisationSecondaryEmail, ChangeDetailsOrgSecondEmailPage, c)
      e                  <- organisationSecondaryHavePhone.fold(Future.successful(d))(havePhone =>
                              Future.fromTry(d.set(ChangeDetailsOrgSecondHavePhonePage, havePhone))
                            )
      updatedUserAnswers <- setUserAnswer(organisationSecondaryPhone, ChangeDetailsOrgSecondPhoneNumberPage, e)
    } yield updatedUserAnswers
  }
}
