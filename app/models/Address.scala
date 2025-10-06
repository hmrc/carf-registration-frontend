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

case class Address(
    line1: String,
    line2: String,
    postcode: String,
    country: Option[String] = None
) {

  def renderHTML(isUkBased: Boolean): String = {
    val addressLines = Seq(line1, line2, postcode).filter(_.nonEmpty)
    val allLines     = if (isUkBased) {
      addressLines
    } else {
      addressLines ++ country.toSeq
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

  def renderMessage: String =
    Seq(line1, line2, postcode)
      .filter(_.nonEmpty)
      .mkString(", ")
}
