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

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Reads, Writes}

sealed trait JourneyType

case object OrgWithUtr extends JourneyType

case object OrgWithoutId extends JourneyType

case object IndWithNino extends JourneyType

case object IndWithUtr extends JourneyType

case object IndWithoutId extends JourneyType

object JourneyType {

  implicit val reads: Reads[JourneyType] = Reads {
    case JsString("OrgWithUtr")   => JsSuccess(OrgWithUtr)
    case JsString("OrgWithoutId") => JsSuccess(OrgWithoutId)
    case JsString("IndWithNino")  => JsSuccess(IndWithNino)
    case JsString("IndWithUtr")   => JsSuccess(IndWithUtr)
    case JsString("IndWithoutId") => JsSuccess(IndWithoutId)
    case JsString(other)          =>
      JsError(s"Unknown JourneyType: $other")
    case _                        =>
      JsError("JourneyType must be a string")
  }

  implicit val writes: Writes[JourneyType] = Writes {
    case OrgWithUtr   => JsString("OrgWithUtr")
    case OrgWithoutId => JsString("OrgWithoutId")
    case IndWithNino  => JsString("IndWithNino")
    case IndWithUtr   => JsString("IndWithUtr")
    case IndWithoutId => JsString("IndWithoutId")
  }

  // Or if you prefer one implicit:
  implicit val format: Format[JourneyType] = Format(reads, writes)
}
