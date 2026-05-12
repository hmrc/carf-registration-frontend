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

import connectors.SubscriptionConnector
import models.SubscriptionId
import models.requests.{SubscriptionContactDetails, SubscriptionIndividualContact, SubscriptionOrganisationContact, SubscriptionRequest}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import types.ResultT
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

class SubscriptionController @Inject() (
    connector: SubscriptionConnector,
    val controllerComponents: MessagesControllerComponents,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  def updateOrgSubscription(name: String): Action[AnyContent] = Action.async { implicit request =>

    val organisation = SubscriptionOrganisationContact(name)

    val contact = SubscriptionContactDetails(
      individual = None,
      organisation = Some(organisation),
      email = "test@email.com",
      phone = Some("07123456789")
    )

    val subscriptionRequest = SubscriptionRequest(
      gbUser = true,
      idNumber = "XM000123456799",
      idType = "SAFE",
      primaryContact = contact,
      secondaryContact = Some(contact),
      tradingName = None
    )

    connector.updateSubscription(subscriptionRequest).processResponse

  }

  def updateIndvSubscription(email: String): Action[AnyContent] = Action.async { implicit request =>

    val individual = SubscriptionIndividualContact("John", "Doe")
    val contact    = SubscriptionContactDetails(
      individual = Some(individual),
      organisation = None,
      email = email,
      phone = Some("07123456789")
    )

    val subscriptionRequest = SubscriptionRequest(
      gbUser = true,
      idNumber = "XM000123456799",
      idType = "SAFE",
      primaryContact = contact,
      secondaryContact = None,
      tradingName = None
    )

    connector.updateSubscription(subscriptionRequest).processResponse
  }

  extension (result: ResultT[SubscriptionId]) {
    private def processResponse =
      result.value
        .map {
          case Right(subscriptionId) => Ok(s"Subscription ID: $subscriptionId")
          case Left(error)           => Ok(error.toString)
        }
  }
}
