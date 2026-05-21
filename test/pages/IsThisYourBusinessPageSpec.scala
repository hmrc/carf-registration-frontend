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

package pages

import base.SpecBase
import models.*

class IsThisYourBusinessPageSpec extends SpecBase {

  "IsThisYourBusinessPage" - {
    "cleanup" - {
      "must set the match flag to false and the clear safe id when the answer is false" in {
        val testPageDetails = testIsThisYourBusinessPageDetails.copy(pageAnswer = Some(false))
        val ua              = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result          = IsThisYourBusinessPage.cleanup(testPageDetails, ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep the match as false and safe id as none when the answer is false" in {
        val testPageDetails = testIsThisYourBusinessPageDetails.copy(pageAnswer = Some(false))
        val ua              = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result          = IsThisYourBusinessPage.cleanup(testPageDetails, ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep the match as false and safe id as none when the answer is true" in {
        val testPageDetails = testIsThisYourBusinessPageDetails.copy(pageAnswer = Some(true))
        val ua              = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result          = IsThisYourBusinessPage.cleanup(testPageDetails, ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep the match as true and safe id present when the answer is true" in {
        val testPageDetails = testIsThisYourBusinessPageDetails.copy(pageAnswer = Some(true))
        val ua              = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result          = IsThisYourBusinessPage.cleanup(testPageDetails, ua, hasChanged = true).success.value

        result.hasValidMatch mustBe true
        result.safeId        mustBe Some(SafeId(testSafeId))
      }

      "must keep the match as false and safe id as none when the answer is None" in {
        val testPageDetails = testIsThisYourBusinessPageDetails.copy(pageAnswer = None)
        val ua              = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result          = IsThisYourBusinessPage.cleanup(testPageDetails, ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }

      "must keep the match as true and safe id present when the answer is None" in {
        val testPageDetails = testIsThisYourBusinessPageDetails.copy(pageAnswer = None)
        val ua              = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result          = IsThisYourBusinessPage.cleanup(testPageDetails, ua, hasChanged = true).success.value

        result.hasValidMatch mustBe true
        result.safeId        mustBe Some(SafeId(testSafeId))
      }
    }
  }
}
