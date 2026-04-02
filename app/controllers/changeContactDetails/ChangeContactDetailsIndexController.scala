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
import models.UserAnswers
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage}
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
    subscriptionService.displaySubscription(request.subscriptionId) flatMap {
      case Some(subscriptionDetails) =>
        subscriptionDetails.isIndividualRegistrationType match {
          case Some(true)  =>
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
          case Some(false) =>
            for {
              _ <-
                sessionRepository.set(
                  UserAnswers(
                    id = request.userId,
                    changeIsIndividualRegType = Some(false)
                  )
                )
            } yield Redirect(
              routes.PlaceholderController.onPageLoad(
                "Should redirect to /change-contact/organisation/details (CARF-141)"
              )
            )
          case None        =>
            logger.warn(s"[ChangeContactDetailsIndexController] User answers could not be found for request.")
            Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        }
      case None                      =>
        logger.warn(s"[ChangeContactDetailsIndexController] User answers could not be found for request.")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  }
}
