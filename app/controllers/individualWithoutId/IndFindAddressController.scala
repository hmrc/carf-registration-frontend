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

package controllers.individualWithoutId

import controllers.actions.*
import forms.individualWithoutId.IndFindAddressFormProvider
import models.responses.AddressResponse
import models.{IndFindAddress, Mode}
import navigation.Navigator
import pages.AddressLookupPage
import pages.individualWithoutId.IndFindAddressPage
import play.api.Logging
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.AddressLookupService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individualWithoutId.IndFindAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndFindAddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: IndFindAddressFormProvider,
    addressLookupService: AddressLookupService,
    val controllerComponents: MessagesControllerComponents,
    view: IndFindAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[IndFindAddress] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(IndFindAddressPage).fold(form)(form.fill)

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      val formReturned = form.bindFromRequest()
      formReturned
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            addressLookupService
              .postcodeSearch(value.postcode, value.propertyNameOrNumber)
              .flatMap {
                case Left(error) =>
                  logger.error(s"Address lookup service failed: $error")
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                case Right(Nil)  =>
                  val formError =
                    formReturned.withError(FormError("postcode", List("indFindAddress.error.postcode.notFound")))
                  Future.successful(BadRequest(view(formError, mode)))

                case Right(addresses) =>
                  for {
                    updatedAnswers            <- Future.fromTry(request.userAnswers.set(IndFindAddressPage, value))
                    updatedAnswersWithAddress <- Future.fromTry(
                                                   updatedAnswers.set(
                                                     AddressLookupPage,
                                                     addresses
                                                   )
                                                 )
                    _                         <- sessionRepository.set(updatedAnswersWithAddress)
                  } yield Redirect(navigator.nextPage(IndFindAddressPage, mode, updatedAnswersWithAddress))
              }
        )

  }

}
