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

import play.api.libs.json.{Json, OFormat}

case class ChangeContactDetailsAuditEvent(
    individualUpdatedValues: Option[IndividualValues],
    individualOriginalValues: Option[IndividualValues],
    organisationOriginalValues: Option[OrganisationValues],
    organisationUpdatedValues: Option[OrganisationValues]
)

object ChangeContactDetailsAuditEvent {
  implicit val format: OFormat[ChangeContactDetailsAuditEvent] = Json.format[ChangeContactDetailsAuditEvent]
}

case class IndividualValues(
    emailAddress: String,
    contactByPhone: Boolean,
    phoneNumber: Option[String]
)

object IndividualValues {
  implicit val format: OFormat[IndividualValues] = Json.format[IndividualValues]
}

case class OrganisationValues(
    contact1Name: String,
    contact1EmailAddress: String,
    contact1ByPhone: Boolean,
    contact1PhoneNumber: Option[String],
    contact2: Option[Boolean],
    contact2Name: Option[String],
    contact2EmailAddress: Option[String],
    contact2ByPhone: Option[Boolean],
    contact2PhoneNumber: Option[String]
)

object OrganisationValues {
  implicit val format: OFormat[OrganisationValues] = Json.format[OrganisationValues]
}
