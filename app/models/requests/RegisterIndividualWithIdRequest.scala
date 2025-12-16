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

import play.api.libs.json.{Json, OFormat, Writes}

sealed trait RegisterIndividualWithIdRequest {
  def requiresNameMatch: Boolean
  def IDNumber: String
  def IDType: String
  def firstName: String
  def lastName: String
}

object RegisterIndividualWithIdRequest {
  implicit val writes: Writes[RegisterIndividualWithIdRequest] = Writes {
    case r: RegisterIndividualWithIdAndDobRequest =>
      Json.toJson(r)(RegisterIndividualWithIdAndDobRequest.format)

    case r: RegisterIndividualWithIdNoDobRequest =>
      Json.toJson(r)(RegisterIndividualWithIdNoDobRequest.format)
  }
}
case class RegisterIndividualWithIdAndDobRequest(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    dateOfBirth: String,
    firstName: String,
    lastName: String
) extends RegisterIndividualWithIdRequest

object RegisterIndividualWithIdAndDobRequest {
  implicit val format: OFormat[RegisterIndividualWithIdAndDobRequest] =
    Json.format[RegisterIndividualWithIdAndDobRequest]
}

case class RegisterIndividualWithIdNoDobRequest(
    requiresNameMatch: Boolean,
    IDNumber: String,
    IDType: String,
    firstName: String,
    lastName: String
) extends RegisterIndividualWithIdRequest

object RegisterIndividualWithIdNoDobRequest {
  implicit val format: OFormat[RegisterIndividualWithIdNoDobRequest] = Json.format[RegisterIndividualWithIdNoDobRequest]
}
