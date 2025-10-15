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

import models.{Address, Business, UniqueTaxpayerReference}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject() () {

  def getBusinessByUtr(utr: String): Future[Option[Business]] =
    Future.successful {
      // temp implementation as auto-matching not yet implemented
      if (utr.startsWith("1")) {
        // UK business
        Some(
          Business(
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
          Business(
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

  def getBusinessName(
      uniqueTaxpayerReference: UniqueTaxpayerReference,
      businessName: String
  )(implicit ec: ExecutionContext): Future[Option[Business]] =
    getBusinessByUtr(uniqueTaxpayerReference.uniqueTaxPayerReference).map {
      case Some(business) => Some(business.copy(name = businessName))
      case None           => None
    }
}
