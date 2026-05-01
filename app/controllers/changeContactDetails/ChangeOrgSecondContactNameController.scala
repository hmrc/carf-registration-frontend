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
import pages.changeContactDetails.ChangeDetailsOrgSecondNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChangeOrgSecondContactNameView
import forms.organisation.OrganisationSecondContactNameFormProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeOrgSecondContactNameController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    formProvider: OrganisationSecondContactNameFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ChangeOrgSecondContactNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (carfIdRetrieval() andThen changeDetailsDataRequiredAction) { implicit request =>

      val preparedForm = request.userAnswers.get(ChangeDetailsOrgSecondNamePage).fold(form)(form.fill)

      Ok(view(preparedForm, mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(request.userAnswers.set(ChangeDetailsOrgSecondNamePage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(ChangeDetailsOrgSecondNamePage, mode, updatedAnswers))
        )
  }
}
