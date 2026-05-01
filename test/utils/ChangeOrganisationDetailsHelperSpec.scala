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

package utils

import play.api.i18n.Messages
import testUtils.ChangeDetailsTestData
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow

class ChangeOrganisationDetailsHelperSpec extends ChangeDetailsTestData {

  val testHelper                  = new ChangeOrganisationDetailsHelper()
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

  "ChangeOrganisationDetailsHelper" - {
    "getFirstContactDetailsSectionMaybe" - {
      "must return a section when all questions have been answered correctly" in {
        val rows: Seq[SummaryListRow] = testHelper.getFirstContactDetailsSectionMaybe(fullUserAnswers).get
        val expectedKeys: Seq[String] = Seq(
          "First contact name",
          "First contact email address",
          "Can we contact the first contact by phone?",
          "First contact Phone number"
        )
        compareRowsToExpected(expectedKeys, rows)
      }

      "must return a section when all questions have been answered correctly but have phone was false" in {
        val rows: Seq[SummaryListRow] = testHelper.getFirstContactDetailsSectionMaybe(userAnswersNoPhone).get
        val expectedKeys: Seq[String] = Seq(
          "First contact name",
          "First contact email address",
          "Can we contact the first contact by phone?"
        )
        compareRowsToExpected(expectedKeys, rows)
      }

      "must return None if user answers does not contain email address" in {
        val rows: Option[Seq[SummaryListRow]] = testHelper.getFirstContactDetailsSectionMaybe(emptyUserAnswers)

        rows mustBe None
      }

      "must return None if user answers does not contain have phone" in {
        val rows: Option[Seq[SummaryListRow]] =
          testHelper.getFirstContactDetailsSectionMaybe(userAnswersWithoutHavePhone)

        rows mustBe None
      }

      "must return None if user answers does not contain name" in {
        val rows: Option[Seq[SummaryListRow]] =
          testHelper.getFirstContactDetailsSectionMaybe(userAnswersNameMissing)

        rows mustBe None
      }

      "must return None if user answers does not contain phone but have phone is true" in {
        val rows: Option[Seq[SummaryListRow]] = testHelper.getFirstContactDetailsSectionMaybe(userAnswersPhoneMissing)

        rows mustBe None
      }
    }

    "getSecondContactDetailsSectionMaybe" - {
      "must return a section when have second contact is false" in {
        val rows: Seq[SummaryListRow] =
          testHelper.getSecondContactDetailsSectionMaybe(userAnswersWithoutSecondContact).get
        val expectedKeys: Seq[String] = Seq(
          "Do you have a second contact?"
        )
        compareRowsToExpected(expectedKeys, rows)
      }

      "must return a section when all questions have been answered correctly" in {
        val rows: Seq[SummaryListRow] = testHelper.getSecondContactDetailsSectionMaybe(fullSecondContactUserAnswers).get
        val expectedKeys: Seq[String] = Seq(
          "Do you have a second contact?",
          "Second contact name",
          "Second contact email address",
          "Can we contact the second contact by phone?",
          "Second contact Phone number"
        )
        compareRowsToExpected(expectedKeys, rows)
      }

      "must return a section when all questions have been answered correctly but have phone was false" in {
        val rows: Seq[SummaryListRow] =
          testHelper.getSecondContactDetailsSectionMaybe(userAnswersSecondContactNoPhone).get
        val expectedKeys: Seq[String] = Seq(
          "Do you have a second contact?",
          "Second contact name",
          "Second contact email address",
          "Can we contact the second contact by phone?"
        )
        compareRowsToExpected(expectedKeys, rows)
      }

      "must return None if user answers does not contain email address" in {
        val rows: Option[Seq[SummaryListRow]] = testHelper.getSecondContactDetailsSectionMaybe(emptyUserAnswers)

        rows mustBe None
      }

      "must return None if user answers does not contain have phone" in {
        val rows: Option[Seq[SummaryListRow]] =
          testHelper.getSecondContactDetailsSectionMaybe(userAnswersSecondContactWithoutHavePhone)

        rows mustBe None
      }

      "must return None if user answers does not contain name" in {
        val rows: Option[Seq[SummaryListRow]] =
          testHelper.getSecondContactDetailsSectionMaybe(userAnswersSecondContactNameMissing)

        rows mustBe None
      }

      "must return None if user answers does not contain phone but have phone is true" in {
        val rows: Option[Seq[SummaryListRow]] =
          testHelper.getSecondContactDetailsSectionMaybe(userAnswersSecondPhoneMissing)

        rows mustBe None
      }

      "must return None if user answers does not contain other answers but have second contact is true" in {
        val rows: Option[Seq[SummaryListRow]] =
          testHelper.getSecondContactDetailsSectionMaybe(userAnswersWithSecondContact)

        rows mustBe None
      }
    }
  }
}
