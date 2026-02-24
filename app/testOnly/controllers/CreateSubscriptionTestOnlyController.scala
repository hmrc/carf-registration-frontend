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

import connectors.SubscriptionConnector
import models.requests.{CreateSubscriptionRequest, SubscriptionContactDetails, SubscriptionIndividualContact, SubscriptionOrganisationContact}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CreateSubscriptionTestOnlyController @Inject() (
    connector: SubscriptionConnector,
    val controllerComponents: MessagesControllerComponents,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  def makeCall(primaryContactName: Option[String], safeId: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>

      val testRequest: CreateSubscriptionRequest = CreateSubscriptionRequest(
        gbUser = false,
        idNumber = safeId.get,
        idType = "SAFE",
        tradingName = Some("Test trading name"),
        primaryContact = SubscriptionContactDetails(
          individual = Some(SubscriptionIndividualContact(primaryContactName.get, "Test last name")),
          organisation = None,
          email = "Test email",
          phone = Some("0123456789")
        ),
        secondaryContact = Some(
          SubscriptionContactDetails(
            individual = None,
            organisation = Some(SubscriptionOrganisationContact("Test org name")),
            email = "Test email",
            phone = None
          )
        )
      )
      connector.createSubscription(testRequest).value map {
        case Right(value) => Ok(s"Success response! \n\n${value.toString}")
        case Left(value)  => Ok(s"Error response! \n\n${value.toString}")
      }
  }
}
