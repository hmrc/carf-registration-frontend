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

trait Transforms {

  protected def stripSpaces(string: String): String =
    string.trim.replaceAll(" ", "")

  protected def validPostCodeFormat(validPostCode: String): String =
    if (!validPostCode.contains(" ")) {
      val tail = validPostCode.substring(validPostCode.length - 3)
      val head = validPostCode.substring(0, validPostCode.length - 3)
      s"$head $tail".toUpperCase
    } else { validPostCode.toUpperCase }

}
