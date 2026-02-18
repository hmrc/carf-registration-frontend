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

package controllers.individualWithoutId

import controllers.actions.*
import controllers.routes
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.AddressLookupPage
import pages.individualWithoutId.{IndReviewConfirmAddressPageForNavigatorOnly, IndWithoutIdUkAddressInUserAnswers}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individualWithoutId.IndReviewConfirmAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndReviewConfirmAddressController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    navigator: Navigator,
    sessionRepository: SessionRepository,
    val controllerComponents: MessagesControllerComponents,
    view: IndReviewConfirmAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>

      val editAddressLink: String =
        controllers.individualWithoutId.routes.IndWithoutIdAddressController
          .onPageLoad(NormalMode)
          .url

      request.userAnswers.get(AddressLookupPage) match {
        case Some(address :: Nil) =>
          Future.successful(Ok(view(address, mode, editAddressLink)))
        case Some(list)           =>
          logger.warn("One address in user answers expected, multiple were found")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))

        case None =>
          logger.warn("No addresses were found in user answers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      request.userAnswers.get(AddressLookupPage) match {
        case Some(addresses) if addresses.nonEmpty =>
          for {
            updatedAnswers <-
              Future.fromTry(request.userAnswers.set(IndWithoutIdUkAddressInUserAnswers, addresses.head))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(IndReviewConfirmAddressPageForNavigatorOnly, mode, updatedAnswers))
        case _                                     =>
          logger.error("No address found in user answers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }

  }

}
