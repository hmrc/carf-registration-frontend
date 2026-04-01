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
import forms.ChangeDetailsIndividualHavePhoneFormProvider
import models.{DataRequestWithSubscriptionId, NormalMode}
import navigation.Navigator
import pages.changeContactDetails.ChangeDetailsIndividualHavePhonePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChangeDetailsIndividualHavePhoneView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeDetailsIndividualHavePhoneController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    formProvider: ChangeDetailsIndividualHavePhoneFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: ChangeDetailsIndividualHavePhoneView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] =
    (carfIdRetrieval() andThen changeDetailsDataRequiredAction) { implicit request =>

      val preparedForm = request.userAnswers.get(ChangeDetailsIndividualHavePhonePage).fold(form)(form.fill)

      Ok(view(preparedForm))
    }

  def onSubmit(): Action[AnyContent] = (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          value =>
            request.userAnswers.get(ChangeDetailsIndividualHavePhonePage) match {
              case Some(oldValue) => handleRedirectionAndUpdateUserAnswers(oldValue, value)
              case None           =>
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
        )
  }

  private def handleRedirectionAndUpdateUserAnswers(
      oldValue: Boolean,
      newValue: Boolean
  )(implicit request: DataRequestWithSubscriptionId[AnyContent]): Future[Result] =
    if (newValue) {
      if (oldValue) {
        Future.successful(
          Redirect(navigator.nextPage(ChangeDetailsIndividualHavePhonePage, NormalMode, request.userAnswers))
        )
      } else {
        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeDetailsIndividualHavePhonePage, newValue))
          _              <- sessionRepository.set(updatedAnswers)
        } yield Redirect(
          controllers.routes.PlaceholderController.onPageLoad(
            "Should redirect to change individual phone number page (CARF-139)"
          )
        )
      }
    } else {
      for {
        // TODO: Remove value when page exists (CARF-139)
        // removedPhone   <- Future.fromTry(request.userAnswers.remove(ChangeDetailsIndividualPhoneNumberPage))
        updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeDetailsIndividualHavePhonePage, newValue))
        _              <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(ChangeDetailsIndividualHavePhonePage, NormalMode, updatedAnswers))
    }
}
