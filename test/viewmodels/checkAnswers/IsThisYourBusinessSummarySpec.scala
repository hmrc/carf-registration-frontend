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

package viewmodels.checkAnswers

import base.SpecBase
import models.{IsThisYourBusinessPageDetails, UserAnswers}
import pages.IsThisYourBusinessPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow

class IsThisYourBusinessSummarySpec extends SpecBase {

  val testUserAnswers: UserAnswers =
    emptyUserAnswers.withPage(IsThisYourBusinessPage, IsThisYourBusinessPageDetails(testBusinessDetails, None))
  implicit val messages: Messages  = stubMessages()

  "IsThisYourBusinessSummary" - {
    "when isCtAutoMatched in user answers is true" - {
      "change link should take the user to the Unable To Change Business Name page" in {
        val testRow: Option[SummaryListRow] =
          IsThisYourBusinessSummary.row(testUserAnswers.copy(isCtAutoMatched = true))

        val href = testRow.get.actions.head.items.head.href
        href mustBe controllers.organisation.routes.UnableToChangeBusinessController.onPageLoad().url
      }
    }
    "when isCtAutoMatched in user answers is false" - {
      "change link should take the user to the start of the journey (Index Page)" in {
        val testRow: Option[SummaryListRow] = IsThisYourBusinessSummary.row(testUserAnswers)

        val href = testRow.get.actions.head.items.head.href
        href mustBe controllers.routes.IndexController.onPageLoad().url
      }
    }
  }
}
