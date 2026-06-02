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
import models.{ChangeMode, Mode, NormalMode, ProvideMode}
import pages.individual.IndividualEmailPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individual.RegisterIdentityConfirmedView

import javax.inject.Inject

class RegisterIdentityConfirmedController @Inject() (
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    override val messagesApi: MessagesApi,
    val controllerComponents: MessagesControllerComponents,
    view: RegisterIdentityConfirmedView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  private lazy val emailUrl: String =
    controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode).url

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      val continueUrl =
        mode match {
          case NormalMode => emailUrl
          case ChangeMode =>
            if request.userAnswers.get(IndividualEmailPage).isDefined then
              controllers.routes.CheckYourAnswersController.onPageLoad().url
            else emailUrl
          case _          =>
            logger.warn("Unsupported navigation for Provide mode on Identity confirmed page")
            controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      Ok(view(continueUrl))
    }
}
