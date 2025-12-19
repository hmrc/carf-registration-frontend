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

package forms.mappings

import models.Country
import play.api.data.FormError
import play.api.data.format.Formatter

class PostcodeFormatter(
    countryList: Seq[Country],
    lengthKey: String,
    invalidCharKey: String,
    requiredCrownKey: String,
    invalidFormatCrownKey: String,
    invalidRealCrownKey: String
) extends Formatter[Option[String]] {

  private val crownDependencies                = Seq("GG", "JE", "IM")
  private val realCrownDependencyPostcodeRegex = "^((GY([1-9]|10))|(JE[1-4])|(IM([1-9]|99))) ?[0-9][A-Z]{2}$"
  private val postcodeCharsRegex               = "^[A-Z0-9 ]*$"

  private def normalise(countryCode: Option[String], postcode: String): String = {
    val isCrownDependency = countryCode.exists(crownDependencies.contains)

    if (isCrownDependency) {
      val noSpaces = postcode.replaceAll("\\s", "").toUpperCase
      if (noSpaces.length > 3) {
        val (start, end) = noSpaces.splitAt(noSpaces.length - 3)
        s"$start $end"
      } else {
        noSpaces
      }
    } else {
      postcode.trim
    }
  }

  private def validation(countryCode: Option[String], postcode: String): Either[Seq[FormError], Option[String]] = {
    val isCrownDependency = countryCode.exists(crownDependencies.contains)
    val cc                = countryCode.getOrElse("")

    if (postcode.isEmpty) {
      if (isCrownDependency) Left(Seq(FormError("postcode", requiredCrownKey)))
      else Right(None)
    } else if (postcode.length > 10) {
      Left(Seq(FormError("postcode", lengthKey)))
    } else if (isCrownDependency && !postcode.matches(postcodeCharsRegex)) {
      Left(Seq(FormError("postcode", invalidCharKey)))
    } else if (isCrownDependency && !postcode.startsWith(cc)) {
      Left(Seq(FormError("postcode", invalidFormatCrownKey)))
    } else if (isCrownDependency && !postcode.matches(realCrownDependencyPostcodeRegex)) {
      Left(Seq(FormError("postcode", invalidRealCrownKey)))
    } else {
      Right(Some(postcode))
    }
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] = {
    val countryCode        = data.get("country")
    val normalisedPostcode = normalise(countryCode, data.getOrElse(key, ""))
    validation(countryCode, normalisedPostcode)
  }

  override def unbind(key: String, value: Option[String]): Map[String, String] =
    Map(key -> value.getOrElse(""))
}
