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

import models.requests.{AddressDetails, ContactDetails, RegisterIndividualWithoutIdRequest}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IndividualWithoutIdController @Inject() (
    registrationService: RegistrationService,
    val controllerComponents: MessagesControllerComponents,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  def registerWithoutId: Action[AnyContent] = Action.async { implicit request =>

    val testRequest =
      RegisterIndividualWithoutIdRequest(
        firstName = "John",
        lastName = "Doe",
        dateOfBirth = "1990-01-01",
        address = AddressDetails(
          addressLine1 = "1 Test Street",
          addressLine2 = None,
          addressLine3 = None,
          townOrCity = "London",
          postalCode = Some("SW1A1AA"),
          countryCode = "GB"
        ),
        contactDetails = ContactDetails(
          emailAddress = "john.doe@test.com",
          phoneNumber = Some("07123456789")
        )
      )

    registrationService.individualWithoutId(testRequest).map {
      case Right(response) =>
        Ok(s"SAFE ID returned: ${response.safeId}")

      case Left(error) =>
        Ok(s"Error returned: $error")
    }
  }
}
