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

import play.api.libs.json.{Json, OFormat}

case class AddressRegistrationResponse(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    addressLine4: Option[String],
    postalCode: Option[String],
    countryCode: String
)

extension (address: AddressRegistrationResponse) {
  def isUkBased: Boolean = address.countryCode == "GB"

  def renderHTML: String = {
    val addressLines = Seq(
      Some(address.addressLine1),
      address.addressLine2,
      address.addressLine3,
      address.addressLine4,
      address.postalCode
    ).flatten.filter(_.nonEmpty)

    val allLines = if (isUkBased) {
      addressLines
    } else {
      addressLines :+ address.countryCode
    }

    val htmlLines = allLines.zipWithIndex.map { case (line, index) =>
      if (index < allLines.length - 1) {
        s"""<span class="govuk-!-margin-bottom-0">$line</span>"""
      } else {
        line
      }
    }

    htmlLines.mkString("<br>")
  }
}

object AddressRegistrationResponse {
  implicit val format: OFormat[AddressRegistrationResponse] = Json.format[AddressRegistrationResponse]
}
