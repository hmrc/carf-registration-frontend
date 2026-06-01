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

class OrganisationHaveSecondContactPageSpec extends SpecBase {

  "OrganisationHaveSecondContactPage" - {
    "cleanup" - {
      "must remove all second contact data when answer changes to no" in {
        val ua = emptyUserAnswers
          .withPage(OrganisationSecondContactNamePage, "Timmy")
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        val result = OrganisationHaveSecondContactPage.cleanup(false, ua, hasChanged = true).success.value

        result.get(OrganisationSecondContactNamePage)        mustBe None
        result.get(OrganisationSecondContactEmailPage)       mustBe None
        result.get(OrganisationSecondContactHavePhonePage)   mustBe None
        result.get(OrganisationSecondContactPhoneNumberPage) mustBe None
      }

      "must keep all second contact data when answer changes to yes" in {
        val ua = emptyUserAnswers
          .withPage(OrganisationSecondContactNamePage, "Timmy")
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        val result = OrganisationHaveSecondContactPage.cleanup(true, ua, hasChanged = true).success.value

        result.get(OrganisationSecondContactNamePage)        mustBe Some("Timmy")
        result.get(OrganisationSecondContactEmailPage)       mustBe Some(testEmail)
        result.get(OrganisationSecondContactHavePhonePage)   mustBe Some(true)
        result.get(OrganisationSecondContactPhoneNumberPage) mustBe Some(testPhone)
      }

      "must keep all second contact data when answer has not changed" in {
        val ua = emptyUserAnswers
          .withPage(OrganisationSecondContactNamePage, "Timmy")
          .withPage(OrganisationSecondContactEmailPage, testEmail)
          .withPage(OrganisationSecondContactHavePhonePage, true)
          .withPage(OrganisationSecondContactPhoneNumberPage, testPhone)

        val result = OrganisationHaveSecondContactPage.cleanup(false, ua, hasChanged = false).success.value

        result.get(OrganisationSecondContactNamePage)        mustBe Some("Timmy")
        result.get(OrganisationSecondContactEmailPage)       mustBe Some(testEmail)
        result.get(OrganisationSecondContactHavePhonePage)   mustBe Some(true)
        result.get(OrganisationSecondContactPhoneNumberPage) mustBe Some(testPhone)
      }
    }
  }
}
