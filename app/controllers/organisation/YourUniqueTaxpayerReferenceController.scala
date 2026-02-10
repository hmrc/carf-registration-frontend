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
import models.RegistrationType.*
import models.{Mode, RegistrationType, UniqueTaxpayerReference}
import navigation.Navigator
import pages.organisation.{RegistrationTypePage, YourUniqueTaxpayerReferencePage}
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
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    formProvider: YourUniqueTaxpayerReferenceFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: YourUniqueTaxpayerReferenceView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData) { implicit request =>
      request.userAnswers
        .get(RegistrationTypePage)
        .flatMap(getTaxTypeMessageKey) match {
        case Some(messageKey) =>
          val form         = formProvider(messageKey)
          val preparedForm = request.userAnswers.get(YourUniqueTaxpayerReferencePage).fold(form)(form.fill)

          Ok(view(preparedForm, mode, messageKey))

        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      request.userAnswers
        .get(RegistrationTypePage)
        .flatMap(getTaxTypeMessageKey) match {
        case Some(messageKey) =>
          val form = formProvider(messageKey)
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, messageKey))),
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
        case None             => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def getTaxTypeMessageKey(registrationType: RegistrationType): Option[String] =
    registrationType match {
      case LimitedCompany | Trust => Some("yourUniqueTaxpayerReference.ltdUnincorporated")
      case Partnership | LLP      => Some("yourUniqueTaxpayerReference.partnershipLlp")
      case SoleTrader             => Some("yourUniqueTaxpayerReference.soleTrader")
      case Individual             => None
    }
}
