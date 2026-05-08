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
import models.{DataRequestWithSubscriptionId, Mode, NormalMode, ProvideMode}
import navigation.Navigator
import pages.changeContactDetails.{ChangeDetailsOrgFirstNamePage, ChangeDetailsOrgHaveSecondContactPage, ChangeDetailsOrgSecondEmailPage, ChangeDetailsOrgSecondHavePhonePage, ChangeDetailsOrgSecondNamePage, ChangeDetailsOrgSecondPhoneNumberPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChangeOrgHaveSecondContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeOrgHaveSecondContactController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    formProvider: OrganisationHaveSecondContactFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ChangeOrgHaveSecondContactView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction) {
    implicit request =>
      val preparedForm = request.userAnswers
        .get(ChangeDetailsOrgHaveSecondContactPage)
        .fold(form)(form.fill)

      request.userAnswers.get(ChangeDetailsOrgFirstNamePage) match {
        case Some(firstContactName) => Ok(view(preparedForm, mode, firstContactName))
        case None                   => Redirect(controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad())
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers
              .get(ChangeDetailsOrgFirstNamePage)
              .fold(
                Future.successful(
                  Redirect(controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad())
                )
              )(name => Future.successful(BadRequest(view(formWithErrors, mode, name)))),
          value =>
            val oldValue = request.userAnswers.get(ChangeDetailsOrgHaveSecondContactPage)
            handleRedirectionAndUpdateUserAnswers(oldValue, value, mode)
        )
  }

  private def handleRedirectionAndUpdateUserAnswers(
      oldValue: Option[Boolean],
      newValue: Boolean,
      mode: Mode
  )(implicit request: DataRequestWithSubscriptionId[AnyContent]): Future[Result] =
    (oldValue, newValue) match {
      case (Some(true), true) =>
        for {
          updatedAnswers <-
            Future.fromTry(request.userAnswers.set(ChangeDetailsOrgHaveSecondContactPage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(ChangeDetailsOrgHaveSecondContactPage, mode, updatedAnswers))
      case (_, true)          =>
        for {
          updatedAnswers <-
            Future.fromTry(request.userAnswers.set(ChangeDetailsOrgHaveSecondContactPage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          controllers.changeContactDetails.routes.ChangeOrgSecondContactNameController
            .onPageLoad(ProvideMode)
        )

      case _ =>
        for {
          removedSecondContactName      <-
            Future.fromTry(request.userAnswers.remove(ChangeDetailsOrgSecondNamePage))
          removedEmail                  <-
            Future.fromTry(removedSecondContactName.remove(ChangeDetailsOrgSecondEmailPage))
          removedSecondContactHavePhone <- Future.fromTry(removedEmail.remove(ChangeDetailsOrgSecondHavePhonePage))
          removedSecondContactPhone     <-
            Future.fromTry(removedSecondContactHavePhone.remove(ChangeDetailsOrgSecondPhoneNumberPage))
          updatedAnswers                <-
            Future.fromTry(removedSecondContactPhone.set(ChangeDetailsOrgHaveSecondContactPage, newValue))
          _                             <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(ChangeDetailsOrgHaveSecondContactPage, mode, updatedAnswers))
    }

}
