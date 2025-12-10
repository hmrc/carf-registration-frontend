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

package testOnly.controllers

import connectors.AddressLookupConnector
import models.requests.SearchByPostcodeRequest
import models.responses.AddressResponse
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddressLookupController @Inject() (
    connector: AddressLookupConnector,
    val controllerComponents: MessagesControllerComponents,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  def searchByPostcode(postcode: String, filter: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      connector.searchByPostcode(SearchByPostcodeRequest(postcode = postcode, filter = filter)) map {
        case Right(value) => Ok(formatAddressResponses(value))
        case Left(value)  => Ok(value.toString)
      }

  }

  private def formatAddressResponses(responses: Seq[AddressResponse]): String =
    s"Total responses = ${responses.length}\n\n\n" +
      responses.zipWithIndex
        .map { case (resp, idx) =>
          val a = resp.address

          s"""
         |#${idx + 1}
         |ID: ${resp.id}
         |Address:
         |  ${a.lines.mkString("\n  ")}
         |Town: ${a.town}
         |Postcode: ${a.postcode}
         |Country: ${a.country.name} (${a.country.code})
         |""".stripMargin.trim
        }
        .mkString("\n\n---\n\n")
}
