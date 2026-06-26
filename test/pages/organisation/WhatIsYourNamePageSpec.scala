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

package pages.organisation

import base.SpecBase
import models.*
import pages.IsThisYourBusinessPage

class WhatIsYourNamePageSpec extends SpecBase {

  "WhatIsYourNamePage" - {
    "removal of IsThisYourBusinessPage" - {
      "when the answer has changed" in {
        val userAnswers = emptyUserAnswers.withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails)

        val result = WhatIsYourNamePage.cleanup(Name("Timmy", "Jimmy"), userAnswers, hasChanged = true).success.value

        result.get(IsThisYourBusinessPage) mustBe None
      }

      "when the answer has not changed" in {
        val userAnswers = emptyUserAnswers.withPage(IsThisYourBusinessPage, testIsThisYourBusinessPageDetails)

        val result = WhatIsYourNamePage.cleanup(Name("Timmy", "Jimmy"), userAnswers, hasChanged = false).success.value

        result.get(IsThisYourBusinessPage) mustBe Some(testIsThisYourBusinessPageDetails)
      }
    }

    "cleanup" - {
      "must set the match flag to false and the clear safe id when the answer has changed" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result = WhatIsYourNamePage.cleanup(Name("Timmy", "Jimmy"), ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep the match as false and safe id as none when the answer has changed" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result = WhatIsYourNamePage.cleanup(Name("Timmy", "Jimmy"), ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep the match as false and safe id as none when the answer has not changed" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result = WhatIsYourNamePage.cleanup(Name("Timmy", "Jimmy"), ua, hasChanged = false).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep the match as true and safe id as present when the answer has not changed" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result = WhatIsYourNamePage.cleanup(Name("Timmy", "Jimmy"), ua, hasChanged = false).success.value

        result.hasValidMatch mustBe true
        result.safeId        mustBe Some(SafeId(testSafeId))
      }
    }
  }
}
