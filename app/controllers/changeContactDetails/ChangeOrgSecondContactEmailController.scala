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

package controllers.changeContactDetails

import controllers.actions.*
import models.Mode
import navigation.Navigator
import pages.changeContactDetails.{ChangeDetailsOrgSecondEmailPage, ChangeDetailsOrgSecondNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChangeOrgSecondContactEmailView
import forms.organisation.OrganisationSecondContactEmailFormProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeOrgSecondContactEmailController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    formProvider: OrganisationSecondContactEmailFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ChangeOrgSecondContactEmailView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (carfIdRetrieval() andThen changeDetailsDataRequiredAction) { implicit request =>

      val preparedForm = request.userAnswers.get(ChangeDetailsOrgSecondEmailPage).fold(form)(form.fill)

      request.userAnswers
        .get(ChangeDetailsOrgSecondNamePage)
        .fold(Redirect(controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad()))(name =>
          Ok(view(preparedForm, mode, name))
        )
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              request.userAnswers
                .get(ChangeDetailsOrgSecondNamePage)
                .fold(Redirect(controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad()))(
                  name => BadRequest(view(formWithErrors, mode, name))
                )
            ),
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(request.userAnswers.set(ChangeDetailsOrgSecondEmailPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(ChangeDetailsOrgSecondEmailPage, mode, updatedAnswers))
        )
  }
}
