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

package services

import connectors.RegistrationConnector
import models.error.ApiError
import models.requests.RegisterIndividualWithIdRequest
import models.{Address, BusinessDetails, IndividualDetails}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject() (connector: RegistrationConnector)(implicit ec: ExecutionContext) {

  def getIndividualByNino(ninoProxy: String)(implicit hc: HeaderCarrier): Future[Option[IndividualDetails]] =
    connector
      .individualWithNino(
        request = RegisterIndividualWithIdRequest(
          requiresNameMatch = true,
          // TODO: Replace it with actual NINO CARF-164
          IDNumber = ninoProxy,
          IDType = "NINO",
          dateOfBirth = "test-dob",
          firstName = "john",
          lastName = "doe"
        )
      )
      .value
      .flatMap {
        case Right(response)              =>
          Future.successful(
            Some(
              IndividualDetails(
                safeId = response.safeId,
                firstName = response.firstName,
                lastName = response.lastName,
                middleName = response.middleName,
                address = response.address
              )
            )
          )
        case Left(ApiError.NotFoundError) => Future.successful(None)
        case Left(error)                  => Future.failed(new Exception("Unexpected Error!"))
      }

  def getBusinessByUtr(utr: String): Future[Option[BusinessDetails]] =
    Future.successful {
      // temp implementation as auto-matching not yet implemented
      if (utr.startsWith("1")) {
        // UK business
        Some(
          BusinessDetails(
            name = "Agent ABC Ltd",
            address = Address(
              addressLine1 = "2 High Street",
              addressLine2 = Some("Birmingham"),
              addressLine3 = None,
              addressLine4 = None,
              postalCode = Some("B23 2AZ"),
              countryCode = "GB"
            )
          )
        )
      } else if (utr.startsWith("2")) {
        // Non-UK business
        Some(
          BusinessDetails(
            name = "International Ltd",
            address = Address(
              addressLine1 = "3 Apple Street",
              addressLine2 = Some("New York"),
              addressLine3 = None,
              addressLine4 = None,
              postalCode = Some("11722"),
              countryCode = "US"
            )
          )
        )
      } else {
        // Business not found
        None
      }
    }
}
