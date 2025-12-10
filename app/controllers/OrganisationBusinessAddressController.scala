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

package controllers

import controllers.actions._
import forms.OrganisationBusinessAddressFormProvider
import models.{Country, Mode}
import navigation.Navigator
import pages.OrganisationBusinessAddressPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CountryListFactory
import views.html.OrganisationBusinessAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganisationBusinessAddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: OrganisationBusinessAddressFormProvider,
    val controllerComponents: MessagesControllerComponents,
    countryListFactory: CountryListFactory,
    view: OrganisationBusinessAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  private def getCountries(form: Form[_]): Seq[SelectItem] =
    countryListFactory.countryList match {
      case Some(countries: Seq[Country]) => countryListFactory.countrySelectList(form.data, countries)
      case _                             => Seq.empty
    }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(OrganisationBusinessAddressPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, getCountries(preparedForm)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, getCountries(formWithErrors)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationBusinessAddressPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(OrganisationBusinessAddressPage, mode, updatedAnswers))
        )
  }
}
