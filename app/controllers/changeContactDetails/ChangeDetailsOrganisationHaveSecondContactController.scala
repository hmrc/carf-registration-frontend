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

import controllers.actions.{CarfIdRetrievalAction, ChangeDetailsDataRequiredAction}
import forms.organisation.OrganisationHaveSecondContactFormProvider
import models.NormalMode
import navigation.Navigator
import pages.changeContactDetails.{ChangeDetailsFirstContactNamePage, ChangeDetailsOrganisationHaveSecondContactPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChangeDetailsOrganisationHaveSecondContactView
import models.Mode

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeDetailsOrganisationHaveSecondContactController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    formProvider: OrganisationHaveSecondContactFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ChangeDetailsOrganisationHaveSecondContactView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction) {
    implicit request =>
      val preparedForm = request.userAnswers
        .get(ChangeDetailsOrganisationHaveSecondContactPage)
        .fold(form)(form.fill)

      request.userAnswers.get(ChangeDetailsFirstContactNamePage) match {
        case Some(firstContactName) => Ok(view(preparedForm, mode, firstContactName))
        case None                   => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(ChangeDetailsFirstContactNamePage) match {
              case Some(firstContactName) => Future.successful(BadRequest(view(formWithErrors, mode, firstContactName)))
              case None                   => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            },
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                                  request.userAnswers.set(ChangeDetailsOrganisationHaveSecondContactPage, value)
                                )
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(ChangeDetailsOrganisationHaveSecondContactPage, mode, updatedAnswers)
            )
        )
  }
}
