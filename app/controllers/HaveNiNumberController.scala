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
import forms.HaveNiNumberFormProvider
import models.{Mode, UniqueTaxpayerReference}
import navigation.Navigator
import pages.HaveNiNumberPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.HaveNiNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HaveNiNumberController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: HaveNiNumberFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: HaveNiNumberView,
    service: RegistrationService,
    retrieveCtUTR: CtUtrRetrievalAction
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen retrieveCtUTR() andThen getData() andThen requireData) { implicit request =>

      // TODO: Replace utr.get with NINO when doing CARF-164
      service
        .getIndividualByNino(request.utr.getOrElse(UniqueTaxpayerReference("123")).uniqueTaxPayerReference)
        .map(a => logger.info(s"%%% LOOK HERE %%% \n-> $a"))

      val preparedForm = request.userAnswers.get(HaveNiNumberPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HaveNiNumberPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(HaveNiNumberPage, mode, updatedAnswers))
        )
  }
}
