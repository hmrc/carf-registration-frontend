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
import models.error.ApiError.ApplicationError
import models.error.DataError
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow

class ChangeDetailsHelperSpec extends SpecBase {

  val testHelper                  = new ChangeDetailsHelper()
  implicit val messages: Messages = messages(app)

  def compareRowsToExpected(
      expectedKeys: Seq[String],
      actualRows: Seq[SummaryListRow]
  ): Unit = {

    val actualKeys            = actualRows.map(_.key.content)
    val formattedExpectedKeys = expectedKeys.map(key => Text(key))

    withClue(s"""
                |Expected table keys to match in order
                |Expected: $formattedExpectedKeys
                |Actual:   $actualKeys
                |
                |""".stripMargin) {

      actualKeys must have size formattedExpectedKeys.size
      actualKeys must contain theSameElementsInOrderAs formattedExpectedKeys
    }
  }

  "ChangeDetailsHelper" - {
    "getFirstContactDetailsSectionMaybe" - {
      "must return a section when all questions have been answered correctly" in new TestData {
        val rows: Seq[SummaryListRow] = testHelper.getFirstContactDetailsSectionMaybe(fullUserAnswers).get
        val expectedKeys: Seq[String] = Seq(
          "Email address",
          "Can we contact you by phone?",
          "Phone number"
        )
        compareRowsToExpected(expectedKeys, rows)
      }
      "must return a section when all questions have been answered correctly but have phone was false" in new TestData {
        val rows: Seq[SummaryListRow] = testHelper.getFirstContactDetailsSectionMaybe(fullUserAnswersNoPhone).get
        val expectedKeys: Seq[String] = Seq(
          "Email address",
          "Can we contact you by phone?"
        )
        compareRowsToExpected(expectedKeys, rows)
      }
      "must return None if user answers does not contain email address" in new TestData {
        val rows: Option[Seq[SummaryListRow]] = testHelper.getFirstContactDetailsSectionMaybe(emptyUserAnswers)

        rows mustBe None
      }
      "must return None if user answers does not contain have phone" in new TestData {
        val rows: Option[Seq[SummaryListRow]] =
          testHelper.getFirstContactDetailsSectionMaybe(userAnswersWithoutHavePhone)

        rows mustBe None
      }
      "must return None if user answers does not contain phone but have phone is true" in new TestData {
        val rows: Option[Seq[SummaryListRow]] = testHelper.getFirstContactDetailsSectionMaybe(userAnswersWithoutPhone)

        rows mustBe None
      }
    }
  }

  class TestData {
    val userAnswersWithoutHavePhone: UserAnswers =
      emptyUserAnswers.withPage(ChangeDetailsIndividualEmailPage, testEmail)

    val userAnswersWithoutPhone: UserAnswers = emptyUserAnswers
      .withPage(ChangeDetailsIndividualEmailPage, testEmail)
      .withPage(ChangeDetailsIndividualHavePhonePage, true)

    val fullUserAnswers: UserAnswers = emptyUserAnswers
      .withPage(ChangeDetailsIndividualEmailPage, testEmail)
      .withPage(ChangeDetailsIndividualHavePhonePage, true)
      .withPage(ChangeDetailsIndividualPhoneNumberPage, testPhone)

    val fullUserAnswersNoPhone: UserAnswers = emptyUserAnswers
      .withPage(ChangeDetailsIndividualEmailPage, testEmail)
      .withPage(ChangeDetailsIndividualHavePhonePage, false)

  }

}
