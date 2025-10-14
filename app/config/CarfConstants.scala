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
object CarfConstants {
  val businessNameRegex: String                  = "^[A-Za-z0-9&'\\\\^`\\- ]+$"
  val validBusinessNameChars: String             = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789&'\\^`- "
  val validBusinessName105Chars: String          =
    "valid Business Name 105 chars long-&'^`\\abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVQXYZ-456789012345"
  val invalidBusinessNameExceeds105Chars: String =
    "invalid Business Name 106 chars long-&'^`abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVQXYZ-567890123456"
  val validBusinessNameMaxLength: Int            = 105
}
