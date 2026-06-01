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
import models.ChangeMode
import navigation.Navigator
import controllers.routes

class OrganisationSecondContactHavePhonePageSpec extends SpecBase {

  "OrganisationSecondContactHavePhonePage" - {
    "cleanup" - {
      "must remove second contact phone number when answer changes to no" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        val result = OrganisationSecondContactHavePhonePage.cleanup(false, ua, hasChanged = true).success.value

        result.get(OrganisationSecondContactPhoneNumberPage) mustBe None
      }

      "must keep second contact phone number when answer changes to yes" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        val result = OrganisationSecondContactHavePhonePage.cleanup(true, ua, hasChanged = true).success.value

        result.get(OrganisationSecondContactPhoneNumberPage) mustBe Some(testPhone)
      }

      "must keep second contact phone number when answer has not changed" in {
        val ua = emptyUserAnswers.withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        val result = OrganisationSecondContactHavePhonePage.cleanup(false, ua, hasChanged = false).success.value

        result.get(OrganisationSecondContactPhoneNumberPage) mustBe Some(testPhone)
      }
    }
  }
}
