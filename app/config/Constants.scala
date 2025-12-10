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

package config

import scala.util.matching.Regex

object Constants {

  val businessNameRegex: String       = "^[A-Za-z0-9&'\\\\^`\\- ]+$"
  val validBusinessNameMaxLength: Int = 105
  val validBusinessNameMinLength: Int = 1
  val validEmailMaxLength: Int        = 132

  final val individualNameRegex = """^[a-zA-Z &`\-\\'^]*$"""
  final val orgNameRegex        = """^[a-zA-Z0-9 &`\-\'\\\^]*$"""
  final val contactNameRegex    = """^[a-zA-Z0-9 &'\\`^\-]*$"""
  final val phoneNumberRegex    = """^[A-Z0-9 )/(\-*#+]*$""".stripMargin

  final val ninoFormatRegex = """^[A-Z]{2}[0-9]{6}[A-Z]{1}$"""
  final val ninoRegex       =
    "^([ACEHJLMOPRSWXY][A-CEGHJ-NPR-TW-Z]|B[A-CEHJ-NPR-TW-Z]|G[ACEGHJ-NPR-TW-Z]|[KT][A-CEGHJ-MPR-TW-Z]|N[A-CEGHJL-NPR-SW-Z]|Z[A-CEGHJ-NPR-TW-Y])[0-9]{6}[A-D ]$"

  private val utrLengthTen: Int      = 10
  private val utrLengthThirteen: Int = 13
  val maxPhoneLength: Int            = 24

  final val acceptedUtrLengths = Seq(utrLengthTen, utrLengthThirteen)

  val ukTimeZoneStringId = "Europe/London"
}
