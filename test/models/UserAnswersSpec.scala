package models

import base.SpecBase

class UserAnswersSpec extends SpecBase {

  "UserAnswers" - {
    "clearMatchFlagAndSafeId method" - {
      "should set match flag to false and remove the safe id when they are true and present" in {
        val ua = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result = ua.clearMatchFlagAndSafeId

        result.safeId mustBe None
        result.hasValidMatch mustBe false
      }
      "should keep match flag to false and keep the safe id as removed when they are false and not present" in {
        val ua = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result = ua.clearMatchFlagAndSafeId

        result.safeId mustBe None
        result.hasValidMatch mustBe false
      }
    }
  }
}
