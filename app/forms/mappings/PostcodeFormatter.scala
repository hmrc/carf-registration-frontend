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

package forms.mappings

import models.countries.*
import play.api.data.FormError
import play.api.data.format.Formatter
import utils.PostcodeUtil

case class PostcodeFormatter(
    countryList: Seq[Country],
    lengthKey: String,
    invalidCharKey: String,
    requiredCrownKey: String,
    invalidFormatCrownKey: String,
    invalidRealCrownKey: String
) extends Formatter[Option[String]] {

  private val crownDependencies = Seq("GG", "JE", "IM")

  private val realCrownDependencyPostcodeRegex = Map(
    "GG" -> "^GY([1-9]|10) ?[0-9][A-Z]{2}$",
    "JE" -> "^JE[1-4] ?[0-9][A-Z]{2}$",
    "IM" -> "^IM([1-9]|99) ?[0-9][A-Z]{2}$"
  )

  private val cdPostcodeCharsRegex    = "^[A-Z0-9 ]*$"
  private val nonCdPostcodeCharsRegex = "^[A-Za-z0-9 \\-]*$"
  private val examplePostcode         = "AA1 1AA"

  private def getPostcodePrefix(countryCode: String): String =
    countryCode match {
      case "GG"  => "GY"
      case other => other
    }

  private def validation(countryCode: Option[String], postcode: String): Either[Seq[FormError], Option[String]] = {
    val isCrownDependency = countryCode.exists(crownDependencies.contains)
    val cc                = countryCode.getOrElse("")

    (isCrownDependency, postcode) match {
      case (true, "")                                        =>
        Left(Seq(FormError("postcode", requiredCrownKey)))
      case (_, p) if p.length > 10                           =>
        Left(Seq(FormError("postcode", lengthKey)))
      case (true, p) if !p.matches(cdPostcodeCharsRegex)     =>
        Left(Seq(FormError("postcode", invalidCharKey)))
      case (false, p) if !p.matches(nonCdPostcodeCharsRegex) =>
        Left(Seq(FormError("postcode", invalidCharKey)))
      case (true, p) if p == examplePostcode                 =>
        Left(Seq(FormError("postcode", invalidRealCrownKey)))
      case (true, p) if !p.startsWith(getPostcodePrefix(cc)) =>
        Left(Seq(FormError("postcode", invalidFormatCrownKey)))
      case (true, p)                                         =>
        realCrownDependencyPostcodeRegex.get(cc) match {
          case Some(regex) if p.matches(regex) => Right(Some(p))
          case Some(_)                         => Left(Seq(FormError("postcode", invalidRealCrownKey)))
          case None                            => Left(Seq(FormError("postcode", invalidFormatCrownKey)))
        }
      case _                                                 =>
        Right(Some(postcode))
    }
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] = {
    val countryCode = data.get("country")

    val isCrownDependency  = countryCode.exists(crownDependencies.contains)
    val normalisedPostcode = PostcodeUtil.normalise(isCrownDependency, data.getOrElse(key, ""))

    validation(countryCode, normalisedPostcode)
  }

  override def unbind(key: String, value: Option[String]): Map[String, String] =
    Map(key -> value.getOrElse(""))
}
