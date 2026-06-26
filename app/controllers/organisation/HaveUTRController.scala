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

package controllers.organisation

import controllers.actions.*
import forms.organisation.HaveUTRFormProvider
import models.{ChangeMode, Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.orgWithoutId.OrgWithoutIdBusinessNamePage
import pages.organisation.HaveUTRPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswersHelper
import views.html.organisation.HaveUTRView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HaveUTRController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    formProvider: HaveUTRFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: HaveUTRView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging
    with UserAnswersHelper {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>

      val preparedForm = request.userAnswers.get(HaveUTRPage).fold(form)(form.fill)

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
              updatedAnswers <- Future.fromTry(request.userAnswers.set(HaveUTRPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              handleRedirect(
                mode = mode,
                oldUserAnswers = request.userAnswers,
                newUserAnswers = updatedAnswers,
                newValue = value
              )
            )
        )
  }

  private def handleRedirect(
      mode: Mode,
      oldUserAnswers: UserAnswers,
      newUserAnswers: UserAnswers,
      newValue: Boolean
  ): Call =
    mode match {
      case NormalMode => navigator.nextPage(HaveUTRPage, mode, newUserAnswers)
      case _          =>
        val hasChanged = !oldUserAnswers.get(HaveUTRPage).contains(newValue)
        changeModeNavigation(newValue = newValue, hasChanged = hasChanged, newUserAnswers)
    }

  private def changeModeNavigation(newValue: Boolean, hasChanged: Boolean, newUserAnswers: UserAnswers): Call =
    if (newValue) {
      controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(ChangeMode)
    } else {
      (hasChanged, isSoleTrader(newUserAnswers)) match {
        case (_, true)      => controllers.individual.routes.HaveNiNumberController.onPageLoad(ChangeMode)
        case (true, false)  => controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(NormalMode)
        case (false, false) =>
          if (newUserAnswers.get(OrgWithoutIdBusinessNamePage).isDefined) {
            controllers.routes.CheckYourAnswersController.onPageLoad()
          } else {
            controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(NormalMode)
          }
      }
    }

}
