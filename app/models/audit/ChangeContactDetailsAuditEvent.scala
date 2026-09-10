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
    individualUpdatedInformation: Option[IndividualInformation],
    individualOriginalInformation: Option[IndividualInformation],
    organisationOriginalInformation: Option[OrganisationInformation],
    organisationUpdatedInformation: Option[OrganisationInformation]
)

object ChangeContactDetailsAuditEvent {
  implicit val format: OFormat[ChangeContactDetailsAuditEvent] = Json.format[ChangeContactDetailsAuditEvent]
}

case class IndividualInformation(
    emailAddress: String,
    contactByPhone: Boolean,
    phoneNumber: Option[String]
)

object IndividualInformation {
  implicit val format: OFormat[IndividualInformation] = Json.format[IndividualInformation]
}

case class OrganisationInformation(
    contact1Name: String,
    contact1EmailAddress: String,
    contact1ByPhone: Boolean,
    contact1PhoneNumber: Option[String],
    contact2: Boolean,
    contact2Name: Option[String],
    contact2EmailAddress: Option[String],
    contact2ByPhone: Option[Boolean],
    contact2PhoneNumber: Option[String]
)

object OrganisationInformation {
  implicit val format: OFormat[OrganisationInformation] = Json.format[OrganisationInformation]
}
