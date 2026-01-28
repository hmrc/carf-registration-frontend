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
import forms.individualWithoutId.AddressFormProvider
import models.requests.DataRequest
import models.responses.{AddressRecord, AddressResponse}
import models.{AddressUK, IndFindAddress, Mode}
import navigation.Navigator
import pages.{AddressLookupPage, AddressPage}
import pages.individualWithoutId.IndFindAddressPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CountryListFactory
import views.html.AddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: AddressFormProvider,
    val controllerComponents: MessagesControllerComponents,
    countryListFactory: CountryListFactory,
    view: AddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private final def ukCountries                                      =
    countryListFactory.countryCodesForUkCountries
      .filterNot(_.code == "GB")
      .toSeq
  private final def form: Form[AddressUK]                            = formProvider()
  private final def countryListWithFilledForm(form: Form[AddressUK]) =
    countryListFactory.countrySelectList(form.data, ukCountries)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      val maybeFilledForm = preFillForm

      Future.successful(Ok(view(maybeFilledForm, mode, countryListWithFilledForm(maybeFilledForm))))
  }

  private def preFillForm(implicit request: DataRequest[AnyContent]) =
    request.userAnswers
      .get(AddressPage)
      .fold {
        request.userAnswers.get(AddressLookupPage).flatMap(_.headOption) match {
          case None                                    =>
            form
          case Some(AddressResponse(_, addressRecord)) =>
            val filledAddress = AddressUK(
              addressRecord.lines.headOption.getOrElse(""),
              Some(addressRecord.lines.lift(1).getOrElse("")),
              addressRecord.town,
              None,
              addressRecord.postcode,
              addressRecord.country.code
            )
            form.fill(filledAddress)
        }
      }(form.fill)

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, mode, countryListWithFilledForm(formWithErrors)))
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(AddressPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(AddressPage, mode, updatedAnswers))
        )
  }
}
