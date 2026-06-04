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

package pages.individual

import base.SpecBase
import models.SafeId

import java.time.LocalDate

class RegisterDateOfBirthPageSpec extends SpecBase {

  val (year, month, day)                = (2000, 1, 1)
  private val validBirthDate: LocalDate = LocalDate.of(year, month, day)

  "RegisterDateOfBirthPage" - {
    "cleanup" - {
      "must clear match flag and safe Id when hasChanged is true" in {
        val userAnswers = emptyUserAnswers
          .copy(
            hasValidMatch = true,
            safeId = Some(SafeId("XCARF000000001"))
          )
          .withPage(RegisterDateOfBirthPage, validBirthDate)

        val result = RegisterDateOfBirthPage.cleanup(validBirthDate, userAnswers, true)

        result.get.hasValidMatch mustBe false
        result.get.safeId        mustBe None
      }

      "must not clear match flag and safe Id when hasChanged is false" in {
        val userAnswers = emptyUserAnswers
          .copy(
            hasValidMatch = true,
            safeId = Some(SafeId("XCARF000000001"))
          )
          .withPage(RegisterDateOfBirthPage, validBirthDate)

        val result = RegisterDateOfBirthPage.cleanup(validBirthDate, userAnswers, false)

        result.get.hasValidMatch    mustBe true
        result.get.safeId.isDefined mustBe true
      }
    }
  }
}
