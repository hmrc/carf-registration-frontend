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
import forms.RegisterDateOfBirthFormProvider

import javax.inject.Inject
import models.Mode
import navigation.Navigator
import pages.{NiNumberPage, RegisterDateOfBirthPage, WhatIsYourNameIndividualPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.RegisterDateOfBirthView

import scala.concurrent.{ExecutionContext, Future}

class RegisterDateOfBirthController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: RegisterDateOfBirthFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: RegisterDateOfBirthView,
    service: RegistrationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>
      val form         = formProvider()
      val preparedForm = request.userAnswers.get(RegisterDateOfBirthPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      val form = formProvider()
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value => {
            val updatedAnswersTry = request.userAnswers.set(RegisterDateOfBirthPage, value)
            updatedAnswersTry.fold(
              _ => Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad())),
              updatedAnswers => {
                val maybeNino = updatedAnswers.get(NiNumberPage)
                val maybeName = updatedAnswers.get(WhatIsYourNameIndividualPage)
                (maybeNino, maybeName) match {
                  case (Some(nino), Some(name)) =>
                    service
                      .getIndividualByNino(nino, updatedAnswers)
                      .flatMap {
                        case Some(details) =>
                          for {
                            matchedAnswers <- Future.fromTry(updatedAnswers.set(RegisterDateOfBirthPage, value))
                            _              <- sessionRepository.set(matchedAnswers)
                          } yield Redirect(navigator.nextPage(RegisterDateOfBirthPage, mode, matchedAnswers))
                        case None          =>
                          Future
                            .successful(Redirect(routes.IndWithoutNinoCouldNotConfirmIdentityController.onPageLoad()))
                      }
                      .recover { case ex =>
                        Redirect(routes.JourneyRecoveryController.onPageLoad())
                      }
                  case _                        =>
                    Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
                }
              }
            )
          }
        )
  }
}
