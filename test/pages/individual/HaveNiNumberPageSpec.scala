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
