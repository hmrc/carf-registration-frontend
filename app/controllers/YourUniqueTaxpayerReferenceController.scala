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

import controllers.actions.*
import forms.YourUniqueTaxpayerReferenceFormProvider

import javax.inject.Inject
import models.{Mode, UniqueTaxpayerReference, UserAnswers}
import navigation.Navigator
import pages.YourUniqueTaxpayerReferencePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YourUniqueTaxpayerReferenceView

import scala.concurrent.{ExecutionContext, Future}

class YourUniqueTaxpayerReferenceController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: YourUniqueTaxpayerReferenceFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: YourUniqueTaxpayerReferenceView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[String] = formProvider()
  
  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>
      val taxType = getTaxType(request.userAnswers)
      val form    = formProvider(taxType)

      val preparedForm = request.userAnswers.get(YourUniqueTaxpayerReferencePage) match {
      //val preparedForm = request.userAnswers.flatMap(_.get(YourUniqueTaxpayerReferencePage)) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, taxType))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(YourUniqueTaxpayerReferencePage))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(YourUniqueTaxpayerReferencePage, mode, updatedAnswers))
      )
  }

  private def getTaxType(userAnswers: UserAnswers)(implicit messages: Messages): String =
    userAnswers.get(OrganisationRegistrationType) match {
      case Some(LimitedCompany) | Some(UnincorporatedAssociation) => ("pattern matching, this is in some")
      case _ => messages("pattern matching _")
    }
}
