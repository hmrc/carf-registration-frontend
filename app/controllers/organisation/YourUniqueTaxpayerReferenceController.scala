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

package controllers.organisation

import controllers.actions.*
import forms.organisation.YourUniqueTaxpayerReferenceFormProvider
import models.OrganisationRegistrationType.{LLP, LimitedCompany, Partnership, Trust}
import models.{Mode, OrganisationRegistrationType, UniqueTaxpayerReference, UserAnswers}
import navigation.Navigator
import pages.organisation.{OrganisationRegistrationTypePage, YourUniqueTaxpayerReferencePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.YourUniqueTaxpayerReferenceView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourUniqueTaxpayerReferenceController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: YourUniqueTaxpayerReferenceFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: YourUniqueTaxpayerReferenceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>
      val taxType = getTaxTypeMessageKey(request.userAnswers)
      val form    = formProvider(taxType)

      val preparedForm = request.userAnswers.get(YourUniqueTaxpayerReferencePage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, taxType))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      val taxType = getTaxTypeMessageKey(request.userAnswers)
      val form    = formProvider(taxType)
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, taxType))),
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(
                  request.userAnswers
                    .set(YourUniqueTaxpayerReferencePage, value)
                )
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(YourUniqueTaxpayerReferencePage, mode, updatedAnswers))
        )
  }

  private def getTaxTypeMessageKey(userAnswers: UserAnswers): String =
    userAnswers.get(OrganisationRegistrationTypePage) match {
      case Some(LimitedCompany) | Some(Trust) => "yourUniqueTaxpayerReference.ltdUnincorporated"
      case Some(Partnership) | Some(LLP)      => "yourUniqueTaxpayerReference.partnershipLlp"
      case _                                  => "yourUniqueTaxpayerReference.soleTraderIndividual"
    }
}
