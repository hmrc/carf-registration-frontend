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

enum RegistrationType(val code: String, val messagesKey: String):
  case LimitedCompany extends RegistrationType("0000", "limitedCompany")
  case Partnership extends RegistrationType("0001", "partnership")
  case LLP extends RegistrationType("0002", "llp")
  case Trust extends RegistrationType("0003", "trust")
  case SoleTrader extends RegistrationType("0004", "soleTrader")
  case Individual extends RegistrationType("Individual code not needed", "individual")

object RegistrationType {
  implicit val reads: Reads[RegistrationType] = Reads {
    case JsString(v) =>
      RegistrationType.values
        .find(_.toString == v)
        .map(JsSuccess(_))
        .getOrElse(JsError(s"Unknown RegistrationType: $v"))

    case _ =>
      JsError("RegistrationType must be a string")
  }

  implicit val writes: Writes[RegistrationType] =
    Writes(registrationType => JsString(registrationType.toString))

}
