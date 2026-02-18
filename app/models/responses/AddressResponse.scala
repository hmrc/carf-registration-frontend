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

package models.responses

import models.AddressUk
import models.countries.CountryUk
import models.error.{CarfError, ConversionError}
import play.api.libs.json.{Json, OFormat}

case class AddressResponse(
    id: String,
    address: AddressRecord
    // ISO639-1 code, e.g. 'en' for English
    // see https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes
)

object AddressResponse {
  implicit val format: OFormat[AddressResponse] = Json.format[AddressResponse]

  extension (addressResponse: AddressResponse)
    def toDomainAddressUk: Either[CarfError, AddressUk] = {
      val address = addressResponse.address
      address.lines match {
        case head :: next =>
          Right(
            AddressUk(
              addressLine1 = head,
              addressLine2 = next.headOption,
              addressLine3 = next.lift(1),
              townOrCity = address.town,
              postCode = address.postcode,
              countryUk = CountryUk(code = address.country.code, name = address.country.name)
            )
          )
        case Nil          => Left(ConversionError)
      }
    }
}

case class AddressRecord(
    lines: List[String],
    town: String,
    postcode: String,
    country: CountryRecord
)

extension (addressRecord: AddressRecord) {
  def format: String = {
    val addressLines = addressRecord.lines ++ Seq(
      addressRecord.town,
      addressRecord.postcode,
      addressRecord.country.name
    )
    addressLines.mkString(", ")
  }
}

object AddressRecord {
  implicit val format: OFormat[AddressRecord] = Json.format[AddressRecord]
}

case class CountryRecord(
    // ISO3166-1 or ISO3166-2 code, e.g. "GB" or "GB-ENG" (note that "GB" is the official
    // code for UK although "UK" is a reserved synonym and may be used instead)
    // See https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
    // and https://en.wikipedia.org/wiki/ISO_3166-2:GB
    code: String,
    // The printable name for the country, e.g. "United Kingdom"
    name: String
)

object CountryRecord {
  implicit val format: OFormat[CountryRecord] = Json.format[CountryRecord]
}
