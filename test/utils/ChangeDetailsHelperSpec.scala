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

    "getHasChanged" - {
      "must return false when (nothing) has changed" in {
        val fullUserAnswers: UserAnswers = emptyUserAnswers
          .copy(displaySubscriptionResponse = Some(testIndividualDisplaySubscriptionResponse(hasPhone = true)))
          .withPage(ChangeDetailsIndividualHavePhonePage, true)
          .withPage(ChangeDetailsIndividualPhoneNumberPage, testPhone)

        val result = testHelper.getHasChanged(Some(testEmail), Some(true), fullUserAnswers)

        result mustBe Right(false)

      }

      "must return true when (email) has changed" in {
        val fullUserAnswers: UserAnswers = emptyUserAnswers
          .copy(displaySubscriptionResponse = Some(testIndividualDisplaySubscriptionResponse(hasPhone = true)))
          .withPage(ChangeDetailsIndividualHavePhonePage, true)
          .withPage(ChangeDetailsIndividualPhoneNumberPage, testPhone)

        val result = testHelper.getHasChanged(Some("new@domain.com"), Some(true), fullUserAnswers)

        result mustBe Right(true)

      }

      "must return true when (have phone true -> false) has changed" in {
        val fullUserAnswers: UserAnswers = emptyUserAnswers
          .copy(displaySubscriptionResponse = Some(testIndividualDisplaySubscriptionResponse(hasPhone = true)))
          .withPage(ChangeDetailsIndividualPhoneNumberPage, testPhone)

        val result = testHelper.getHasChanged(Some(testEmail), Some(false), fullUserAnswers)

        result mustBe Right(true)

      }

      "must return true when (have phone false -> true) has changed" in {
        val fullUserAnswers: UserAnswers = emptyUserAnswers
          .copy(displaySubscriptionResponse = Some(testIndividualDisplaySubscriptionResponse(hasPhone = false)))
          .withPage(ChangeDetailsIndividualPhoneNumberPage, testPhone)

        val result = testHelper.getHasChanged(Some(testEmail), Some(true), fullUserAnswers)

        result mustBe Right(true)

      }

      "must return left when (have phone false -> true) has changed but ChangeDetailsIndividualPhoneNumberPage is not supplied" in {
        val fullUserAnswers: UserAnswers = emptyUserAnswers
          .copy(displaySubscriptionResponse = Some(testIndividualDisplaySubscriptionResponse(hasPhone = false)))

        val result = testHelper.getHasChanged(Some(testEmail), Some(true), fullUserAnswers)

        result mustBe Left(DataError)

      }

      "must return Left when displaySubscriptionResponse is not supplied" in {
        val fullUserAnswers: UserAnswers = emptyUserAnswers

        val result = testHelper.getHasChanged(Some(testEmail), Some(true), fullUserAnswers)

        result mustBe Left(ApplicationError)

      }
    }

    "decideContinueUrl" - {
      "must return None when no data is missing" in {

        val fullUserAnswers: UserAnswers = emptyUserAnswers
          .withPage(ChangeDetailsIndividualPhoneNumberPage, testPhone)

        val result = testHelper.decideContinueUrl(Some(testEmail), Some(true), fullUserAnswers)

        result mustBe None
      }

      "must return email page's url when email is missing" in {

        val fullUserAnswers: UserAnswers = emptyUserAnswers

        val result = testHelper.decideContinueUrl(None, Some(false), emptyUserAnswers)

        result mustBe Some(
          controllers.changeContactDetails.routes.ChangeIndividualEmailController.onPageLoad().url
        )
      }

      "must return contact by phone page's Url when havePhone is missing" in {

        val result = testHelper.decideContinueUrl(Some(testEmail), None, emptyUserAnswers)

        result mustBe Some(
          controllers.routes.PlaceholderController
            .onPageLoad("Should redirect to change contact by phone page (CARF-138)")
            .url
        )
      }

      "must return phone number page's url when phone number is missing" in {

        val result = testHelper.decideContinueUrl(None, Some(true), emptyUserAnswers)

        result mustBe Some(
          controllers.changeContactDetails.routes.ChangeIndividualEmailController.onPageLoad().url
        )
      }

      "must return email page's url when all data is missing" in {

        val result = testHelper.decideContinueUrl(None, None, emptyUserAnswers)

        result mustBe Some(
          controllers.changeContactDetails.routes.ChangeIndividualEmailController.onPageLoad().url
        )
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
