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

import controllers.actions.{CarfIdRetrievalAction, ChangeDetailsDataRequiredAction}
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeDetailsManipulateUserAnswersController @Inject() (
    carfIdRetrieval: CarfIdRetrievalAction,
    changeDetailsDataRequiredAction: ChangeDetailsDataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    sessionRepository: SessionRepository,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  def changeUserAnswers(key: String, value: String): Action[AnyContent] =
    (carfIdRetrieval() andThen changeDetailsDataRequiredAction).async { implicit request =>
      val keyToChange = key match {
        case "email"       => ChangeDetailsIndividualEmailPage
        case "phoneNumber" => ChangeDetailsIndividualPhoneNumberPage
        case "havePhone"   => ChangeDetailsIndividualPhoneNumberPage
        case _             => throw new Exception("not a recognised key to change")
      }

      val newUa = key match {
        case "email" | "phoneNumber" =>
          Future.fromTry(
            request.userAnswers.set(keyToChange, value)
          )
        case "havePhone"             =>
          Future.fromTry(
            request.userAnswers.set(ChangeDetailsIndividualHavePhonePage, if (value.toUpperCase == "Y") true else false)
          )
        case _                       => throw new Exception("not a recognised key")
      }
      for {
        updatedAnswers <- newUa
        _              <- sessionRepository.set(updatedAnswers)
      } yield Ok(
        s"You successfully manipulated user answers with { key: $key }, { value: $value }<br>" +
          s"""To return to the page please click: <a href="${controllers.changeContactDetails.routes.ChangeIndividualContactDetailsController
              .onPageLoad()}">Return</a>"""
      ).as(HTML)

    }
}
