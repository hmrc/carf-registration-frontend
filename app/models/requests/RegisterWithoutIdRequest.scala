package models.requests

import play.api.libs.json.{Json, OFormat, Writes}

sealed trait RegisterWithoutIdRequest

object RegisterWithoutIdRequest {
  implicit val writes: Writes[RegisterWithoutIdRequest] = Writes {
    case ind: RegisterIndividualWithoutIdRequest   => Json.toJson(ind)
    case org: RegisterOrganisationWithoutIdRequest => Json.toJson(org)
  }
}

case class RegisterIndividualWithoutIdRequest(
    firstName: String,
    lastName: String,
    dateOfBirth: String,
    address: AddressDetails,
    contactDetails: ContactDetails
) extends RegisterWithoutIdRequest

object RegisterIndividualWithoutIdRequest {
  implicit val format: OFormat[RegisterIndividualWithoutIdRequest] = Json.format[RegisterIndividualWithoutIdRequest]
}

case class RegisterOrganisationWithoutIdRequest(
    organisationName: String,
    address: AddressDetails,
    contactDetails: ContactDetails
) extends RegisterWithoutIdRequest

object RegisterOrganisationWithoutIdRequest {
  implicit val format: OFormat[RegisterOrganisationWithoutIdRequest] = Json.format[RegisterOrganisationWithoutIdRequest]
}
