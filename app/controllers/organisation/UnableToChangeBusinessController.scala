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

package controllers.organisation

import config.FrontendAppConfig
import controllers.actions.*
import models.{BusinessDetails, IsThisYourBusinessPageDetails}
import pages.IsThisYourBusinessPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.organisation.UnableToChangeBusinessView

import javax.inject.Inject

class UnableToChangeBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: UnableToChangeBusinessView,
    appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen requireData) { implicit request =>
    val signOutNoSurveyUrl = appConfig.signOutNoSurveyUrl
    val loginContinueUrl   = appConfig.loginContinueUrl

    request.userAnswers.get(IsThisYourBusinessPage) match {
      case Some(pageDetails: IsThisYourBusinessPageDetails) =>
        Ok(view(pageDetails.businessDetails, signOutNoSurveyUrl, loginContinueUrl))
      case None                                             =>
        logger.warn(s"[UnableToChangeBusinessController] Error! Business details are missing from user answers!")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  }
}
