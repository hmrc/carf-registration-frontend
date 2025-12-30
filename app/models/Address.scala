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

import play.api.libs.json.{Json, OFormat}

case class Address(
    addressLine1: String,
    addressLine2: Option[String],
    addressLine3: Option[String],
    addressLine4: Option[String],
    postalCode: Option[String],
    countryCode: String
) {

  def isUkBased: Boolean = countryCode == "GB"

  val renderHTML: String = {
    val addressLines = Seq(
      Some(addressLine1),
      addressLine2,
      addressLine3,
      addressLine4,
      postalCode
    ).flatten.filter(_.nonEmpty)

    val allLines = if (isUkBased) {
      addressLines
    } else {
      addressLines :+ countryCode
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

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]
}
