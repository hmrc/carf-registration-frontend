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
import forms.WhatIsTheNameOfYourBusinessFormProvider
import models.OrganisationRegistrationType.*
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.{OrganisationRegistrationTypePage, WhatIsTheNameOfYourBusinessPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.WhatIsTheNameOfYourBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatIsTheNameOfYourBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: WhatIsTheNameOfYourBusinessFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: WhatIsTheNameOfYourBusinessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>
      val taxType      = getBusinessTypeMessageKey(request.userAnswers)
      val form         = formProvider(taxType)
      val preparedForm = request.userAnswers.get(WhatIsTheNameOfYourBusinessPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, mode, taxType))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      val taxType = getBusinessTypeMessageKey(request.userAnswers)
      val form    = formProvider(taxType)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, taxType))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(WhatIsTheNameOfYourBusinessPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(WhatIsTheNameOfYourBusinessPage, mode, updatedAnswers))
        )
  }

  private def getBusinessTypeMessageKey(userAnswers: UserAnswers): String =
    userAnswers.get(OrganisationRegistrationTypePage) match {
      case Some(LimitedCompany) | Some(LLP) => "whatIsTheNameOfYourBusiness.ltdLpLlp"
      case Some(Partnership)                => "whatIsTheNameOfYourBusiness.partnership"
      case _                                => "whatIsTheNameOfYourBusiness.unincorporatedAssociationTrust"
    }
}
