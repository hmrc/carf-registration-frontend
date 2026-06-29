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
import models.countries.CountryUk
import pages.individualWithoutId.*

class HaveNiNumberPageSpec extends SpecBase {

  val address = AddressUk(
    "1 Test Street",
    Some("Line 2"),
    None,
    "Testtown",
    "BB00 0BB",
    CountryUk("GB", "United Kingdom")
  )

  "HaveNiNumberPage" - {
    "cleanup" - {
      "must remove no-route answers when answer changes to yes" in {
        val ua = emptyUserAnswers
          .withPage(IndFindAddressAdditionalCallUa, true)
          .withPage(IndFindAddressPage, testIndFindAddress)
          .withPage(WhereDoYouLivePage, true)
          .withPage(AddressLookupPage, Seq(AddressAndUPRN(address, testUPRN)))
          .withPage(IndWithoutNinoNamePage, Name("Timmy", "Turner"))
          .withPage(IndWithoutIdAddressNonUkPage, testIndWithoutIdAddressNonUk)
          .withPage(IndWithoutIdAddressPagePrePop, testAddressUk)
          .withPage(IndWithoutIdChooseAddressPage, "test")
          .withPage(IndWithoutIdDateOfBirthPage, testDob)
          .withPage(IndWithoutIdSelectedChooseAddressPage, testAddressUk)
          .withPage(IndWithoutIdUkAddressInUserAnswers, testAddressUk)

        val result = HaveNiNumberPage.cleanup(true, ua, hasChanged = true).success.value

        result.get(IndFindAddressAdditionalCallUa)        mustBe None
        result.get(IndFindAddressPage)                    mustBe None
        result.get(WhereDoYouLivePage)                    mustBe None
        result.get(AddressLookupPage)                     mustBe None
        result.get(IndWithoutNinoNamePage)                mustBe None
        result.get(IndWithoutIdAddressNonUkPage)          mustBe None
        result.get(IndWithoutIdAddressPagePrePop)         mustBe None
        result.get(IndWithoutIdChooseAddressPage)         mustBe None
        result.get(IndWithoutIdDateOfBirthPage)           mustBe None
        result.get(IndWithoutIdSelectedChooseAddressPage) mustBe None
        result.get(IndWithoutIdUkAddressInUserAnswers)    mustBe None
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

      "must keep all answers when answer has not changed" in {
        val ua = emptyUserAnswers
          .withPage(NiNumberPage, "AA123456A")
          .withPage(WhatIsYourNameIndividualPage, Name("Timmy", "McFly"))
          .withPage(RegisterDateOfBirthPage, testDob)

        val result = HaveNiNumberPage.cleanup(true, ua, hasChanged = false).success.value

        result.get(NiNumberPage)                 mustBe Some("AA123456A")
        result.get(WhatIsYourNameIndividualPage) mustBe Some(Name("Timmy", "McFly"))
        result.get(RegisterDateOfBirthPage)      mustBe Some(testDob)
      }
    }
  }
}
