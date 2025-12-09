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

import config.FrontendAppConfig
import controllers.actions._
import pages.organisation.{OrganisationRegistrationTypePage, YourUniqueTaxpayerReferencePage}
import javax.inject.Inject
import pages.WhatIsTheNameOfYourBusinessPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.BusinessNotIdentifiedView

class BusinessNotIdentifiedController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: BusinessNotIdentifiedView,
    appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    val organisationType = request.userAnswers.get(OrganisationRegistrationTypePage)
    val utr              = request.userAnswers.get(YourUniqueTaxpayerReferencePage)
    val businessName     = request.userAnswers.get(WhatIsTheNameOfYourBusinessPage)

    (utr, businessName) match {
      case (Some(utrValue), Some(nameValue)) =>
        Ok(view(utrValue.uniqueTaxPayerReference, nameValue, organisationType, appConfig))
      case _                                 =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
