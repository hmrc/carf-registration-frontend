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

enum JourneyType:
  case OrgWithUtr
  case OrgWithoutId
  case IndWithNino
  case IndWithUtr
  case IndWithoutId

object JourneyType {

  implicit val reads: Reads[JourneyType] = Reads {
    case JsString(value) =>
      JourneyType.values
        .find(_.toString == value)
        .map(JsSuccess(_))
        .getOrElse(JsError(s"Unknown JourneyType: $value"))

    case _ =>
      JsError("JourneyType must be a string")
  }

  implicit val writes: Writes[JourneyType] =
    Writes(jt => JsString(jt.toString))

  implicit val format: Format[JourneyType] =
    Format(reads, writes)
}
