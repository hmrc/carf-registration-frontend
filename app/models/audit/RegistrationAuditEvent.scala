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

package models.audit

import models.RegistrationType
import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.auth.core.AffinityGroup

case class RegistrationAuditEvent(
    affinityGroup: AffinityGroup,
    registeredAs: RegistrationType,
    registeredUkAddress: Option[Boolean],
    hasUtr: Option[Boolean],
    hasNINO: Option[Boolean],
    soleTraderWithUTRJourney: Option[UtrJourneyAuditEvent],
    organisationWithIdJourney: Option[OrganisationWithIdJourney],
    organisationWithoutIdJourney: Option[OrganisationWithoutIdJourney],
    withNinoJourney: Option[WithNinoJourney],
    individualWithoutIdJourney: Option[IndividualWithoutIdJourney],
    individualContactDetails: Option[IndividualContactDetails],
    organisationContactDetails: Option[OrganisationContactDetails]
)

object RegistrationAuditEvent {
  implicit val format: OFormat[RegistrationAuditEvent] = Json.format[RegistrationAuditEvent]
}

extension (event: RegistrationAuditEvent) {
  def toJson: JsValue = Json.toJson(event)
}

case class UtrJourneyAuditEvent(
    utr: String,
    firstName: String,
    lastName: String,
    isThisYourBusiness: Boolean
)

object UtrJourneyAuditEvent {
  implicit val format: OFormat[UtrJourneyAuditEvent] = Json.format[UtrJourneyAuditEvent]
}

case class OrganisationWithIdJourney(
    utr: String,
    businessName: String,
    isThisYourBusiness: Boolean
)

object OrganisationWithIdJourney {
  implicit val format: OFormat[OrganisationWithIdJourney] = Json.format[OrganisationWithIdJourney]
}

case class OrganisationWithoutIdJourney(
    businessName: String,
    haveDifferentBusinessTradeName: Boolean,
    businessTradeName: Option[String],
    businessAddressLine1: String,
    businessAddressLine2: Option[String],
    businessTown: String,
    businessRegion: Option[String],
    businessPostcode: Option[String],
    businessCountry: String
)

object OrganisationWithoutIdJourney {
  implicit val format: OFormat[OrganisationWithoutIdJourney] = Json.format[OrganisationWithoutIdJourney]
}

case class WithNinoJourney(
    nino: String,
    firstName: String,
    lastName: String,
    dateOfBirth: String
)

object WithNinoJourney {
  implicit val format: OFormat[WithNinoJourney] = Json.format[WithNinoJourney]
}

case class IndividualWithoutIdJourney(
    firstName: String,
    lastName: String,
    dateOfBirth: String,
    residentOfUkOrCrownDependency: Boolean,
    findYourAddress: Option[String],
    propertyNameOrNumber: Option[String],
    chooseYourAddress: Option[String],
    UPRN: Option[String],
    addressLineOne: String,
    addressLineTwo: Option[String],
    addressLineThree: Option[String],
    town: String,
    region: Option[String],
    postalCode: Option[String],
    country: String
)

object IndividualWithoutIdJourney {
  implicit val format: OFormat[IndividualWithoutIdJourney] = Json.format[IndividualWithoutIdJourney]
}

case class IndividualContactDetails(
    email: String,
    contactByPhone: Boolean,
    phoneNumber: Option[
      String
    ]
)

object IndividualContactDetails {
  implicit val format: OFormat[IndividualContactDetails] = Json.format[IndividualContactDetails]
}

case class OrganisationContactDetails(
    contact1Name: String,
    contact1EmailAddress: String,
    contact1ByPhone: Boolean,
    contact1PhoneNumber: Option[String],
    contact2: Boolean,
    contact2Name: Option[String],
    contact2EmailAddress: Option[String],
    contact2ByPhone: Option[Boolean],
    contact2PhoneNumber: Option[
      String
    ]
)

object OrganisationContactDetails {
  implicit val format: OFormat[OrganisationContactDetails] = Json.format[OrganisationContactDetails]
}
