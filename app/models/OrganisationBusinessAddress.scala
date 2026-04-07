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

package models

import play.api.libs.json.*
import models.countries.*
import models.requests.AddressDetails

import scala.collection.immutable.Seq

case class OrganisationBusinessAddress(
    addressLine1: String,
    addressLine2: Option[String],
    townOrCity: String,
    region: Option[String],
    postcode: Option[String],
    country: Country
)

extension (address: OrganisationBusinessAddress) {
  def toAddressDetailsOrg: AddressDetails = {
    val addressOptionalLines = Seq(address.addressLine2, address.region).flatten
    AddressDetails(
      addressLine1 = address.addressLine1,
      addressLine2 = addressOptionalLines.headOption,
      addressLine3 = addressOptionalLines.lift(1),
      townOrCity = address.townOrCity,
      postalCode = address.postcode,
      countryCode = address.country.code
    )
  }
}

object OrganisationBusinessAddress {
  implicit val format: OFormat[OrganisationBusinessAddress] = Json.format[OrganisationBusinessAddress]
}
