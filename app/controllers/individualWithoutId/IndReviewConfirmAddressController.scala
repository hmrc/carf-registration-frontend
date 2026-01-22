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

package controllers.individualWithoutId

import controllers.actions.*
import controllers.routes
import forms.individualWithoutId.IndReviewConfirmAddressFormProvider
import models.Mode
import models.responses.AddressResponse
import navigation.Navigator
import pages.AddressLookupPage
import pages.individualWithoutId.IndReviewConfirmAddressPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individualWithoutId.IndReviewConfirmAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndReviewConfirmAddressController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: IndReviewConfirmAddressFormProvider,
    navigator: Navigator,
    sessionRepository: SessionRepository,
    val controllerComponents: MessagesControllerComponents,
    view: IndReviewConfirmAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      val editAddressLink: String =
        controllers.routes.PlaceholderController
          .onPageLoad("Must redirect to /register/individual-without-id/address")
          .url

      request.userAnswers.get(AddressLookupPage).flatMap(_.headOption) match {
        case Some(address) => Future.successful(Ok(view(form, address, mode, editAddressLink)))
        case _             =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(AddressLookupPage) match {
              case Some(addresses) if addresses.nonEmpty =>
                val editAddressLink = controllers.routes.PlaceholderController
                  .onPageLoad("Must redirect to /register/individual-without-id/address")
                  .url
                Future.successful(BadRequest(view(formWithErrors, addresses.head, mode, editAddressLink)))
              case _                                     =>
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            },
          value =>
            request.userAnswers.get(AddressLookupPage) match {
              case Some(addresses) if addresses.nonEmpty =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(IndReviewConfirmAddressPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(IndReviewConfirmAddressPage, mode, updatedAnswers))
              case _                                     =>
                logger.error("No address found in user answers")
                Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
            }
        )
  }

}
