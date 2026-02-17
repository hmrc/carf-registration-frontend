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

package models

import models.countries.{Country, CountryUk}
import play.api.libs.json.{Json, OFormat}

/** @param addressLine1
  *   Address Line 1
  * @param addressLine2
  *   Address Line 2
  * @param addressLine3
  *   Address Line 3
  * @param townOrCity
  *   Town or city
  * @param postCode
  *   post code e.g. NW1 5RT
  * @param countryUk
  *   country e.g. CountryUk("GB", "United Kingdom")
  */
case class AddressUk(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    townOrCity: String,
    postCode: String,
    countryUk: CountryUk
)

extension (address: AddressUk) {
  def renderHTML: String = {
    val addressLines = Seq(
      Some(address.addressLine1),
      address.addressLine2,
      address.addressLine3,
      Some(address.townOrCity),
      Some(address.postCode),
      Some(address.countryUk.name)
    ).flatten.filter(_.nonEmpty)

    val htmlLines = addressLines.zipWithIndex.map { case (line, index) =>
      if (index < addressLines.length - 1) {
        s"""<span class="govuk-!-margin-bottom-0">$line</span>"""
      } else {
        line
      }
    }

    htmlLines.mkString("<br>")
  }
}

object AddressUk {
  implicit val format: OFormat[AddressUk] = Json.format[AddressUk]
}
