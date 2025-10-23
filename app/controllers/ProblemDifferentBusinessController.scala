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
import pages.IsThisYourBusinessPage
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ProblemDifferentBusinessView
import scala.concurrent.{ExecutionContext, Future}

class ProblemDifferentBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: ProblemDifferentBusinessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify() andThen getData() andThen requireData).async { implicit request =>
    request.userAnswers.get(IsThisYourBusinessPage) match {
      case Some(existingPageDetails) =>
        val businessName = existingPageDetails.name
        val address      = existingPageDetails.address
        Future.successful(Ok(view(businessName, address)))
      case None                      =>
        logger.warn(
          "No business details found in UserAnswers during form submission. " + "Redirecting to journey recovery."
        )
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
