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
import models.{Address, IsThisYourBusinessPageDetails, UserAnswers}
import pages.IsThisYourBusinessPage
import pages.organisation.*
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import viewmodels.Section

class CheckYourAnswersHelperSpec extends SpecBase {

  val testHelper                  = new CheckYourAnswersHelper()
  implicit val messages: Messages = messages(app)

  def compareRowsAndTitleToExpected(
      expectedTitle: String,
      expectedKeys: Seq[String],
      section: Section
  ): Unit = {

    val actualKeys            = section.rows.map(_.key.content)
    val formattedExpectedKeys = expectedKeys.map(key => Text(key))

    withClue(s"""
         |Expected table keys to match in order
         |Expected: $formattedExpectedKeys
         |Actual:   $actualKeys
         |
         |""".stripMargin) {

      expectedTitle mustEqual section.sectionName
      actualKeys         must have size formattedExpectedKeys.size
      actualKeys         must contain theSameElementsInOrderAs formattedExpectedKeys
    }
  }

  "CheckYourAnswersHelper" - {
    "getBusinessDetailsSectionMaybe" - {
      "must return a section when the IsThisYourBusinessPage has been answered" in new TestData {
        val section: Section          = testHelper.getBusinessDetailsSectionMaybe(testUserAnswersWithBusinessDetails).get
        val expectedTitle             = "Business details"
        val expectedKeys: Seq[String] = Seq("Your business")

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return None when the IsThisYourBusinessPage has NOT been answered" in {
        val section = testHelper.getBusinessDetailsSectionMaybe(emptyUserAnswers)

        section mustBe None
      }
    }

    "getFirstContactDetailsSectionMaybe" - {
      "must return a section when all questions have been answered" in new TestData {
        val section: Section          = testHelper.getFirstContactDetailsSectionMaybe(testUserAnswersWithFirstContactDetails).get
        val expectedTitle             = "First contact"
        val expectedKeys: Seq[String] = Seq(
          "First contact name",
          "First contact email address",
          "Can we contact the first contact by phone?",
          "First contact phone number"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return a section when all questions have been answered except phone number contact" in new TestData {
        val section: Section          =
          testHelper.getFirstContactDetailsSectionMaybe(testUserAnswersWithFirstContactDetailsNoPhoneNumber).get
        val expectedTitle             = "First contact"
        val expectedKeys: Seq[String] =
          Seq("First contact name", "First contact email address", "Can we contact the first contact by phone?")

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return None when no answers have been answered" in new TestData {
        val section: Option[Section] = testHelper.getFirstContactDetailsSectionMaybe(emptyUserAnswers)

        section mustBe None
      }

      "must return None when first contact name is not answered" in new TestData {
        val section: Option[Section] = testHelper.getFirstContactDetailsSectionMaybe(
          testUserAnswersWithFirstContactDetails.remove(FirstContactNamePage).success.value
        )

        section mustBe None
      }

      "must return None when first contact email is not answered" in new TestData {
        val section: Option[Section] = testHelper.getFirstContactDetailsSectionMaybe(
          testUserAnswersWithFirstContactDetails.remove(FirstContactEmailPage).success.value
        )

        section mustBe None
      }

      "must return None when can we contact you by phone is not answered" in new TestData {
        val section: Option[Section] = testHelper.getFirstContactDetailsSectionMaybe(
          testUserAnswersWithFirstContactDetails.remove(FirstContactPhonePage).success.value
        )

        section mustBe None
      }

      "must return None when can we contact you by phone is yes but there is no phone number" in new TestData {
        val section: Option[Section] = testHelper.getFirstContactDetailsSectionMaybe(
          testUserAnswersWithFirstContactDetails.remove(FirstContactPhoneNumberPage).success.value
        )

        section mustBe None
      }
    }
    "getSecondContactDetailsSectionMaybe" - {
      "must return a section when there is no second contact" in new TestData {
        val section: Section          =
          testHelper.getSecondContactDetailsSectionMaybe(testUserAnswersWithSecondContactDetailsAsNo).get
        val expectedTitle             = "Second contact"
        val expectedKeys: Seq[String] = Seq(
          "Is there someone else we can contact if your first contact is not available?"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return a section when all questions have been answered" in new TestData {
        val section: Section          =
          testHelper.getSecondContactDetailsSectionMaybe(testUserAnswersWithSecondContactDetails).get
        val expectedTitle             = "Second contact"
        val expectedKeys: Seq[String] = Seq(
          "Is there someone else we can contact if your first contact is not available?",
          "Second contact name",
          "Second contact email address",
          "Can we contact the second contact by phone?",
          "First contact phone number"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return a section when all questions have been answered but can we contact second contact by phone is no" in new TestData {
        val section: Section          =
          testHelper.getSecondContactDetailsSectionMaybe(testUserAnswersWithSecondContactDetailsNoPhoneNumber).get
        val expectedTitle             = "Second contact"
        val expectedKeys: Seq[String] = Seq(
          "Is there someone else we can contact if your first contact is not available?",
          "Second contact name",
          "Second contact email address",
          "Can we contact the second contact by phone?"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return None when 'is there a second contact' is empty" in new TestData {
        val section: Option[Section] = testHelper.getSecondContactDetailsSectionMaybe(emptyUserAnswers)

        section mustBe None
      }

      "must return None when second contact name is not answered" in new TestData {
        val section: Option[Section] = testHelper.getSecondContactDetailsSectionMaybe(
          testUserAnswersWithSecondContactDetails.remove(OrganisationSecondContactNamePage).success.value
        )

        section mustBe None
      }

      "must return None when can we contact second contact you by phone is not answered" in new TestData {
        val section: Option[Section] = testHelper.getSecondContactDetailsSectionMaybe(
          testUserAnswersWithSecondContactDetails.remove(OrganisationSecondContactEmailPage).success.value
        )

        section mustBe None
      }

      "must return None when can we contact you by phone is yes but there is no phone number" in new TestData {
        val section: Option[Section] = testHelper.getSecondContactDetailsSectionMaybe(
          testUserAnswersWithSecondContactDetails.remove(FirstContactPhoneNumberPage).success.value
        )

        section mustBe None
      }
    }
  }

  class TestData {
    val testAddress = Address(
      addressLine1 = "2 Newarre Road",
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      postalCode = None,
      countryCode = "GB"
    )

    val testUserAnswersWithBusinessDetails: UserAnswers = emptyUserAnswers
      .set(
        IsThisYourBusinessPage,
        IsThisYourBusinessPageDetails(name = "TEST NAME", address = testAddress, pageAnswer = Some(true))
      )
      .success
      .value

    val testUserAnswersWithFirstContactDetails: UserAnswers = emptyUserAnswers
      .set(FirstContactNamePage, "TEST NAME")
      .success
      .value
      .set(FirstContactEmailPage, "TEST EMAIL")
      .success
      .value
      .set(FirstContactPhonePage, true)
      .success
      .value
      .set(FirstContactPhoneNumberPage, "123")
      .success
      .value

    val testUserAnswersWithFirstContactDetailsNoPhoneNumber: UserAnswers = emptyUserAnswers
      .set(FirstContactNamePage, "TEST NAME")
      .success
      .value
      .set(FirstContactEmailPage, "TEST EMAIL")
      .success
      .value
      .set(FirstContactPhonePage, false)
      .success
      .value

    val testUserAnswersWithSecondContactDetailsAsNo: UserAnswers = emptyUserAnswers
      .set(OrganisationHaveSecondContactPage, false)
      .success
      .value

    val testUserAnswersWithSecondContactDetails: UserAnswers = emptyUserAnswers
      .set(OrganisationHaveSecondContactPage, true)
      .success
      .value
      .set(OrganisationSecondContactNamePage, "TEST NAME")
      .success
      .value
      .set(OrganisationSecondContactEmailPage, "TEST EMAIL")
      .success
      .value
      .set(OrganisationSecondContactHavePhonePage, true)
      .success
      .value
      .set(FirstContactPhoneNumberPage, "123")
      .success
      .value

    val testUserAnswersWithSecondContactDetailsNoPhoneNumber: UserAnswers = emptyUserAnswers
      .set(OrganisationHaveSecondContactPage, true)
      .success
      .value
      .set(OrganisationSecondContactNamePage, "TEST NAME")
      .success
      .value
      .set(OrganisationSecondContactEmailPage, "TEST EMAIL")
      .success
      .value
      .set(OrganisationSecondContactHavePhonePage, false)
      .success
      .value

  }

}
