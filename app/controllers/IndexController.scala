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

import controllers.actions.{CheckEnrolledToServiceAction, CtUtrRetrievalAction, DataRetrievalAction, IdentifierAction}
import models.UserAnswers

import javax.inject.Inject
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndexView

import scala.concurrent.Future

class IndexController @Inject() (
    val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    checkEnrolment: CheckEnrolledToServiceAction,
    retrieveCtUTR: CtUtrRetrievalAction,
    sessionRepository: SessionRepository,
    getData: DataRetrievalAction,
    view: IndexView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify() andThen checkEnrolment andThen retrieveCtUTR()).async {
    implicit request =>
      request.affinityGroup match {
        case AffinityGroup.Individual =>
          Future.successful(Ok("Take user to: Individual – What Are You Registering As? page (CARF-120)"))
        case _                        =>
          request.utr match {
            case Some(utr) =>
              Future.successful(Ok("User has UTR. Redirect them to Is This Your Business? page (CARF-126)"))
            case None      =>
              Future.successful(
                Ok("We couldn't get a UTR for this user. Redirect them to What Are You Registering As? page (CARF-119)")
              )
          }
      }
  }
}
