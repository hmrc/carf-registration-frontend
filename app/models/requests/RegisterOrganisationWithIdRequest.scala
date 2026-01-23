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

package models.requests

import play.api.libs.json.{Json, OFormat}

sealed trait RegisterOrganisationWithIdRequest {
  val requiresNameMatch: Boolean
  val IDNumber: String
  val IDType: String
}

object RegisterOrganisationWithIdRequest {
  implicit val format: OFormat[RegisterOrganisationWithIdRequest] = Json.format[RegisterOrganisationWithIdRequest]
}

case class RegOrgWithIdNonAutoMatchRequest(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    organisationName: String,
    organisationType: String
) extends RegisterOrganisationWithIdRequest

object RegOrgWithIdNonAutoMatchRequest {
  implicit val format: OFormat[RegOrgWithIdNonAutoMatchRequest] = Json.format[RegOrgWithIdNonAutoMatchRequest]
}

case class RegOrgWithIdCTAutoMatchRequest(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String
) extends RegisterOrganisationWithIdRequest

object RegOrgWithIdCTAutoMatchRequest {
  implicit val format: OFormat[RegOrgWithIdCTAutoMatchRequest] = Json.format[RegOrgWithIdCTAutoMatchRequest]
}
