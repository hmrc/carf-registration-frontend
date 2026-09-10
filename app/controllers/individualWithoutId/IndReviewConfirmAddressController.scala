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
import models.Mode
import navigation.Navigator
import pages.individualWithoutId.{IndReviewConfirmAddressPageForNavigatorOnly, IndWithoutIdAddressPagePrePop, IndWithoutIdUkAddressInUserAnswers}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.LoggerUtil.*
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
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>

      val editAddressLink: String =
        controllers.individualWithoutId.routes.IndWithoutIdAddressController
          .onPageLoad(mode)
          .url

      request.userAnswers.get(IndWithoutIdAddressPagePrePop) match {
        case Some(address) =>
          Ok(view(address, mode, editAddressLink))
        case None          =>
          logWarn("[IndReviewConfirmAddressController][onPageLoad] No address found in user answers")
          Redirect(routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      request.userAnswers.get(IndWithoutIdAddressPagePrePop) match {
        case Some(address) =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IndWithoutIdUkAddressInUserAnswers, address))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(IndReviewConfirmAddressPageForNavigatorOnly, mode, updatedAnswers))
        case None          =>
          logError("[IndReviewConfirmAddressController][onSubmit] No address found in user answers")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

}
