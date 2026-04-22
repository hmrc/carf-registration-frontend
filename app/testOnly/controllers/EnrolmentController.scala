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

package testOnly.controllers

import connectors.EnrolmentConnector
import models.requests.{EnrolmentRequest, Identifier, Verifier}
import models.error.ApiError
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnrolmentController @Inject() (
    connector: EnrolmentConnector,
    val controllerComponents: MessagesControllerComponents,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  def createEnrolment(
      identifierKey: String,
      identifierValue: String,
      verifierKey: String,
      verifierValue: String
  ): Action[AnyContent] = Action.async { implicit request =>
    connector
      .createEnrolment(
        EnrolmentRequest(
          Seq(Identifier(identifierKey, identifierValue)),
          Seq(Verifier(verifierKey, verifierValue))
        )
      )
      .value
      .map {
        case Right(_)                           => Ok("No Content")
        case Left(ApiError.BadRequestError)     => Ok("Bad Request")
        case Left(ApiError.InternalServerError) => Ok("Internal Server Error")
      }

  }
}
