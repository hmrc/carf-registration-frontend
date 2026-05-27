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
import models.{DataRequestWithSubscriptionId, Mode}
import navigation.Navigator
import pages.changeContactDetails.{ChangeDetailsOrgSecondHavePhonePage, ChangeDetailsOrgSecondNamePage, ChangeDetailsOrgSecondPhoneNumberPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChangeOrgSecondContactHavePhoneView
import forms.organisation.OrganisationSecondContactHavePhoneFormProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeOrgSecondContactHavePhoneController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    formProvider: OrganisationSecondContactHavePhoneFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ChangeOrgSecondContactHavePhoneView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (carfIdRetrieval() andThen changeDetailsDataRequiredAction) { implicit request =>

      val preparedForm = request.userAnswers.get(ChangeDetailsOrgSecondHavePhonePage).fold(form)(form.fill)

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
            request.userAnswers
              .get(ChangeDetailsOrgSecondNamePage)
              .fold(
                Future.successful(
                  Redirect(controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad())
                )
              )(name => Future.successful(BadRequest(view(formWithErrors, mode, name)))),
          value =>
            val oldValue = request.userAnswers.get(ChangeDetailsOrgSecondHavePhonePage)
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
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeDetailsOrgSecondHavePhonePage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(ChangeDetailsOrgSecondHavePhonePage, mode, updatedAnswers))

      case (_, true) =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeDetailsOrgSecondHavePhonePage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          controllers.changeContactDetails.routes.ChangeOrgSecondContactPhoneNumberController.onPageLoad(mode)
        )

      case _ =>
        for {
          removedPhone   <- Future.fromTry(request.userAnswers.remove(ChangeDetailsOrgSecondPhoneNumberPage))
          updatedAnswers <- Future.fromTry(removedPhone.set(ChangeDetailsOrgSecondHavePhonePage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(ChangeDetailsOrgSecondHavePhonePage, mode, updatedAnswers))
    }
}
