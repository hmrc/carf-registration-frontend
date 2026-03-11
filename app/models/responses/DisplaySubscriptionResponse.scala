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

package models.responses

import play.api.libs.json.{Json, OFormat}

case class DisplaySubscriptionResponse(success: DisplaySubscriptionSuccess) {
  lazy val isIndividualRegistrationType: Option[Boolean] =
    (
      success.carfSubscriptionDetails.primaryContact.individual.nonEmpty,
      success.carfSubscriptionDetails.primaryContact.organisation.nonEmpty
    ) match {
      case (true, false) => Some(true)
      case (false, true) => Some(false)
      case _             => None
    }
}

object DisplaySubscriptionResponse {
  implicit val format: OFormat[DisplaySubscriptionResponse] = Json.format[DisplaySubscriptionResponse]

  extension (displaySubscriptionResponse: DisplaySubscriptionResponse) {
    def hasIndividualChangedData(email: String, phone: Option[String]): Boolean =
      val primaryContact                 = displaySubscriptionResponse.success.carfSubscriptionDetails.primaryContact
      val hasEmailChanged: Boolean       = !(primaryContact.email == email)
      val hasPhoneNumberChanged: Boolean = !(primaryContact.phone == phone)

      hasEmailChanged | hasPhoneNumberChanged

  }
}

case class DisplaySubscriptionSuccess(processingDate: String, carfSubscriptionDetails: DisplaySubscriptionDetails)

object DisplaySubscriptionSuccess {
  implicit val format: OFormat[DisplaySubscriptionSuccess] = Json.format[DisplaySubscriptionSuccess]
}

case class DisplaySubscriptionDetails(
    tradingName: Option[String],
    gbUser: Boolean,
    primaryContact: DisplaySubscriptionContact,
    secondaryContact: Option[DisplaySubscriptionContact]
)

object DisplaySubscriptionDetails {
  implicit val format: OFormat[DisplaySubscriptionDetails] = Json.format[DisplaySubscriptionDetails]
}

case class DisplaySubscriptionContact(
    individual: Option[DisplaySubscriptionIndividual],
    organisation: Option[DisplaySubscriptionOrganisation],
    email: String,
    phone: Option[String],
    mobile: Option[String]
)

object DisplaySubscriptionContact {
  implicit val format: OFormat[DisplaySubscriptionContact] = Json.format[DisplaySubscriptionContact]
}

case class DisplaySubscriptionIndividual(firstName: String, middleName: Option[String], lastName: String)

object DisplaySubscriptionIndividual {
  implicit val format: OFormat[DisplaySubscriptionIndividual] = Json.format[DisplaySubscriptionIndividual]
}

case class DisplaySubscriptionOrganisation(name: String)

object DisplaySubscriptionOrganisation {
  implicit val format: OFormat[DisplaySubscriptionOrganisation] = Json.format[DisplaySubscriptionOrganisation]
}
