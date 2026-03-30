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

import connectors.RegistrationConnector
import models.requests.{AddressDetails, ContactDetails, RegisterIndividualWithoutIdRequest, RegisterOrganisationWithoutIdRequest}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestOnlyRegWithoutIdController @Inject() (
    registrationService: RegistrationService,
    registrationConnector: RegistrationConnector,
    val controllerComponents: MessagesControllerComponents,
    implicit val executionContext: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  private val testAddress = AddressDetails(
    addressLine1 = "1 Test Street",
    addressLine2 = None,
    addressLine3 = None,
    townOrCity = "London",
    postalCode = Some("SW1A1AA"),
    countryCode = "GB"
  )

  private val testContact = ContactDetails(
    emailAddress = "john.doe@test.com",
    phoneNumber = Some("07123456789")
  )

  def regIndWithoutId(firstName: String): Action[AnyContent] = Action.async { implicit request =>
    val testRequest =
      RegisterIndividualWithoutIdRequest(
        firstName = firstName,
        lastName = "Doe",
        dateOfBirth = "1990-01-01",
        address = testAddress,
        contactDetails = testContact
      )

    registrationConnector.registerIndividualWithoutId(testRequest).value.map {
      case Right(response) =>
        Ok(s"SAFE ID returned: ${response.safeId}")

      case Left(error) =>
        Ok(error.toString)
    }
  }

  def regOrgWithoutId(orgName: String): Action[AnyContent] = Action.async { implicit request =>
    val testRequest =
      RegisterOrganisationWithoutIdRequest(
        organisationName = orgName,
        address = testAddress,
        contactDetails = testContact
      )

    registrationConnector.registerOrganisationWithoutId(testRequest).value.map {
      case Right(response) =>
        Ok(s"SAFE ID returned: ${response.safeId}")

      case Left(error) =>
        Ok(error.toString)
    }
  }
}
