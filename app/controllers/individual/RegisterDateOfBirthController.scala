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

package controllers.individual

import controllers.actions.*
import controllers.routes
import forms.individual.RegisterDateOfBirthFormProvider
import models.error.ApiError
import models.error.ApiError.NotFoundError
import models.requests.DataRequest
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.individual.{NiNumberPage, RegisterDateOfBirthPage, WhatIsYourNameIndividualPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individual.RegisterDateOfBirthView

import java.time.LocalDate
import javax.inject.Inject
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
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>
      val form         = formProvider()
      val preparedForm = request.userAnswers.get(RegisterDateOfBirthPage).fold(form)(form.fill)

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen requireData).async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(RegisterDateOfBirthPage, value))
              _              <- sessionRepository.set(updatedAnswers)
              result         <- handleValidFormSubmission(updatedAnswers, mode)
            } yield result
        )
    }

  private def handleValidFormSubmission(
      updatedAnswers: UserAnswers,
      mode: Mode
  )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val maybeNino = updatedAnswers.get(NiNumberPage)
    val maybeName = updatedAnswers.get(WhatIsYourNameIndividualPage)
    val maybeDob  = updatedAnswers.get(RegisterDateOfBirthPage)

    (maybeNino, maybeName, maybeDob) match {
      case (Some(nino), Some(name), Some(dob)) =>
        service
          .getIndividualByNino(nino, name, dob)
          .flatMap {
            case Right(individualDetails) =>
              Future.successful(Redirect(navigator.nextPage(RegisterDateOfBirthPage, mode, updatedAnswers)))
            case Left(NotFoundError)      =>
              Future.successful(
                Redirect(
                  controllers.individualWithoutId.routes.IndWithoutNinoCouldNotConfirmIdentityController.onPageLoad()
                )
              )
            case Left(error)              =>
              logger.warn(s"Unexpected error. Error: $error")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      case _                                   =>
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
