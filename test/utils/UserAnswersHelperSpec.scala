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

package utils

import base.SpecBase
import models.UserAnswers
import models.IndividualRegistrationType.SoleTrader
import models.JourneyType.{IndWithUtr, OrgWithUtr}
import models.OrganisationRegistrationType.Partnership
import pages.organisation.RegistrationTypePage

class UserAnswersHelperSpec extends SpecBase {

  val testHelper: UserAnswersHelper = new UserAnswersHelper {}

  val userAnswersSoleTrader: UserAnswers   =
    emptyUserAnswers.set(IndividualRegistrationTypePage, SoleTrader).success.value
  val userAnswersOrganisation: UserAnswers =
    emptyUserAnswers.set(RegistrationTypePage, Partnership).success.value

  "UserAnswersHelper" - {
    "getJourneyTypeUtrOnly" - {
      "must return ind with utr when journey is individual with utr" in {
        testHelper.getJourneyTypeUtrOnly(userAnswersSoleTrader) mustBe IndWithUtr
      }
      "must return org with utr when journey is organisation with utr" in {
        testHelper.getJourneyTypeUtrOnly(userAnswersOrganisation) mustBe OrgWithUtr
      }
    }
  }

}
