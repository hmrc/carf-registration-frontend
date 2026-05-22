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
import models.*
import pages.individualWithoutId.{IndWithoutIdDateOfBirthPage, IndWithoutNinoNamePage}

class HaveNiNumberPageSpec extends SpecBase {

  "HaveNiNumberPage" - {
    "cleanup" - {
      "must remove no-route answers when answer changes to yes" in {
        val ua = emptyUserAnswers
          .withPage(IndWithoutNinoNamePage, Name("Timmy", "Turner"))
          .withPage(IndWithoutIdDateOfBirthPage, testDob)

        val result = HaveNiNumberPage.cleanup(true, ua, hasChanged = true).success.value

        result.get(IndWithoutNinoNamePage)      mustBe None
        result.get(IndWithoutIdDateOfBirthPage) mustBe None
      }

      "must remove yes-route answers and clear match flag when answer changes to no" in {
        val ua = emptyUserAnswers
          .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
          .withPage(NiNumberPage, "AA123456A")
          .withPage(WhatIsYourNameIndividualPage, Name("Timmy", "McFly"))
          .withPage(RegisterDateOfBirthPage, testDob)

        val result = HaveNiNumberPage.cleanup(false, ua, hasChanged = true).success.value

        result.hasValidMatch                     mustBe false
        result.safeId                            mustBe None
        result.get(NiNumberPage)                 mustBe None
        result.get(WhatIsYourNameIndividualPage) mustBe None
        result.get(RegisterDateOfBirthPage)      mustBe None
      }
    }
  }
}
