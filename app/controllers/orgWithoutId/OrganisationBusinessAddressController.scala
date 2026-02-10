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

package controllers.orgWithoutId

import controllers.actions.*
import forms.orgWithoutId.OrganisationBusinessAddressFormProvider
import models.{Country, Mode}
import navigation.Navigator
import pages.orgWithoutId.OrganisationBusinessAddressPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CountryListFactory
import views.html.orgWithoutId.OrganisationBusinessAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganisationBusinessAddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    formProvider: OrganisationBusinessAddressFormProvider,
    val controllerComponents: MessagesControllerComponents,
    countryListFactory: CountryListFactory,
    view: OrganisationBusinessAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val countriesList: Option[Seq[Country]] = countryListFactory.countryList.map { countries =>
    countries.filterNot(_.code == "GB")
  }

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>
      countriesList match {
        case Some(countries) =>
          val form         = formProvider(countries)
          val preparedForm = request.userAnswers.get(OrganisationBusinessAddressPage) match {
            case None        => form
            case Some(value) => form.fill(value)
          }
          Future.successful(
            Ok(
              view(
                preparedForm,
                mode,
                countryListFactory.countrySelectList(preparedForm.data, countries)
              )
            )
          )

        case None =>
          logger.error("Could not retrieve countries list from JSON file.")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      countriesList match {
        case Some(countries) =>
          val form = formProvider(countries)
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      mode,
                      countryListFactory.countrySelectList(formWithErrors.data, countries)
                    )
                  )
                ),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(OrganisationBusinessAddressPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(OrganisationBusinessAddressPage, mode, updatedAnswers))
            )
        case None            =>
          logger.error("Could not retrieve countries list from JSON file.")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}
