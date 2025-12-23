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

import config.FrontendAppConfig
import controllers.actions.*
import controllers.routes
import models.OrganisationRegistrationType.SoleTrader
import pages.organisation.{OrganisationRegistrationTypePage, WhatIsTheNameOfYourBusinessPage, YourUniqueTaxpayerReferencePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.BusinessNotIdentifiedView

import javax.inject.Inject

class BusinessNotIdentifiedController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: BusinessNotIdentifiedView,
    appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>

    val companiesHouseSearchUrl: String = appConfig.companiesHouseSearchUrl
    val registrationStartUrl: String    = controllers.routes.IndexController.onPageLoad().url
    val findUTRUrl: String              = appConfig.findUTRUrl
    val aeoiEmailAddress: String        = appConfig.aeoiEmailAddress

    val maybePageInfo = for {
      utr              <- request.userAnswers.get(YourUniqueTaxpayerReferencePage)
      businessName     <- request.userAnswers.get(WhatIsTheNameOfYourBusinessPage)
      organisationType <- request.userAnswers.get(OrganisationRegistrationTypePage)
    } yield (utr.uniqueTaxPayerReference, businessName, organisationType)

    maybePageInfo match {
      case Some((utr, businessName, organisationType)) if !(organisationType == SoleTrader) =>
        Ok(
          view(
            utr,
            businessName,
            organisationType,
            companiesHouseSearchUrl,
            registrationStartUrl,
            findUTRUrl,
            aeoiEmailAddress
          )
        )
      case _                                                                                =>
        logger.warn(
          "Some information was missing from user answers (utr, business name or valid organisation type). Redirecting to journey recovery."
        )
        Redirect(routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
