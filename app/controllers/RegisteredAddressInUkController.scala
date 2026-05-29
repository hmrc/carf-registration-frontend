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

package controllers

import controllers.actions.*
import forms.RegisteredAddressInUkFormProvider
import models.requests.DataRequest
import models.{ChangeMode, Mode}
import navigation.Navigator
import pages.RegisteredAddressInUkPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RegisteredAddressInUkView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisteredAddressInUkController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    formProvider: RegisteredAddressInUkFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: RegisteredAddressInUkView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      if (isInvalidChangeModeRequest(mode)) {
        logger.warn(
          "[RegisteredAddressInUkController] Invalid ChangeMode access for registered-address-in-uk. Redirecting to information missing."
        )
        Redirect(controllers.routes.InformationMissingController.onPageLoad())
      } else {
        val preparedForm =
          request.userAnswers.get(RegisteredAddressInUkPage).fold(form)(form.fill)

        Ok(view(preparedForm, mode))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      if (isInvalidChangeModeRequest(mode)) {
        logger.warn(
          "[RegisteredAddressInUkController] Invalid ChangeMode submit for registered-address-in-uk. Redirecting to information missing."
        )
        Future.successful(Redirect(controllers.routes.InformationMissingController.onPageLoad()))
      } else {
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(RegisteredAddressInUkPage, value))
                _              <- sessionRepository.set(updatedAnswers)
              } yield Redirect(navigator.nextPage(RegisteredAddressInUkPage, mode, updatedAnswers))
          )
      }
  }

  private def isInvalidChangeModeRequest(mode: Mode)(implicit
      request: DataRequest[AnyContent]
  ): Boolean =
    mode == ChangeMode && request.userAnswers.get(RegisteredAddressInUkPage).contains(true)
}
