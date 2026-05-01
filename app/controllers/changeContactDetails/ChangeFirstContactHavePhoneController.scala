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
import forms.changeContactDetails.ChangeFirstContactHavePhoneFormProvider
import models.{DataRequestWithSubscriptionId, Mode}
import navigation.Navigator
import pages.changeContactDetails.{ChangeDetailsFirstContactNamePage, ChangeDetailsFirstContactPhoneNumberPage, ChangeFirstContactHavePhonePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.changeContactDetails.ChangeFirstContactHavePhoneView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeFirstContactHavePhoneController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    formProvider: ChangeFirstContactHavePhoneFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ChangeFirstContactHavePhoneView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ChangeFirstContactHavePhonePage).fold(form)(form.fill)

      request.userAnswers
        .get(ChangeDetailsFirstContactNamePage)
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
              .get(ChangeDetailsFirstContactNamePage)
              .fold(
                Future.successful(
                  Redirect(controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad())
                )
              )(name => Future.successful(BadRequest(view(formWithErrors, mode, name)))),
          value =>
            val oldValue = request.userAnswers.get(ChangeFirstContactHavePhonePage)
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
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeFirstContactHavePhonePage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(ChangeFirstContactHavePhonePage, mode, updatedAnswers))
      case (_, true)          =>
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeFirstContactHavePhonePage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          // TODO: On CARF-497 add the mode to .onPageLoad here
          controllers.changeContactDetails.routes.ChangeFirstContactPhoneNumberController.onPageLoad()
        )
      case _                  =>
        for {
          removedPhone   <- Future.fromTry(request.userAnswers.remove(ChangeDetailsFirstContactPhoneNumberPage))
          updatedAnswers <- Future.fromTry(removedPhone.set(ChangeFirstContactHavePhonePage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(navigator.nextPage(ChangeFirstContactHavePhonePage, mode, updatedAnswers))
    }
}
