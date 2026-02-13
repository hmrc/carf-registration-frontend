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
import models.RegistrationType.{Individual, LLP, SoleTrader}
import models.countries.Country
import models.responses.AddressRegistrationResponse
import models.{AddressUK, BusinessDetails, IndWithoutIdAddressNonUk, IsThisYourBusinessPageDetails, Name, OrganisationBusinessAddress, UserAnswers}
import pages.individual.*
import pages.individualWithoutId.{IndWithoutIdAddressInUserAnswers, IndWithoutIdAddressNonUkPage, IndWithoutIdDateOfBirthPage, IndWithoutNinoNamePage}
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}
import pages.organisation.*
import pages.{IsThisYourBusinessPage, RegisteredAddressInUkPage, WhereDoYouLivePage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import viewmodels.Section

import java.time.LocalDate

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

    "getOrgWithoutIdDetailsMaybe" - {
      "must return a section when all questions have been answered correctly" in new TestData {
        val section: Section          = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswersOrgWithoutId).get
        val expectedTitle             = "Business details"
        val expectedKeys: Seq[String] = Seq(
          "What are you registering as?",
          "Is your registered business address in the UK?",
          "Do you have a Unique Taxpayer Reference?",
          "Business name",
          "Does your organisation trade under a different name?",
          "Trading name",
          "Main business address"
        )
        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return a section when and all questions have been answered correctly but the user doesn't operate under a different trading name" in new TestData {
        val section: Section          = testHelper
          .getOrgWithoutIdDetailsMaybe(testUserAnswersOrgWithoutId.set(HaveTradingNamePage, false).success.value)
          .get
        val expectedTitle             = "Business details"
        val expectedKeys: Seq[String] = Seq(
          "What are you registering as?",
          "Is your registered business address in the UK?",
          "Do you have a Unique Taxpayer Reference?",
          "Business name",
          "Does your organisation trade under a different name?",
          "Main business address"
        )
        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return None if the user says that their business is registered in the uk" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.set(RegisteredAddressInUkPage, true).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None if the user says that they have a UTR" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.set(HaveUTRPage, true).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the What are you registering as? has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.remove(RegistrationTypePage).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the Is your registered address in the UK? has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.remove(RegisteredAddressInUkPage).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the Do you have a utr has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.remove(HaveUTRPage).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the org without id business name has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.remove(OrgWithoutIdBusinessNamePage).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the org without id business address has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.remove(OrganisationBusinessAddressPage).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the org without id does your business trade under a different name page has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.remove(HaveTradingNamePage).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the user says that they operate under a different trading name, but haven't entered one" in new TestData {
        private val testUserAnswers  =
          testUserAnswersOrgWithoutId.set(HaveTradingNamePage, true).success.value.remove(TradingNamePage).success.value
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
    }

    "indWithNinoYourDetailsMaybe" - {
      "must return a section when RegistrationType is SoleTrader and all questions have been answered correctly" in new TestData {
        val section: Section          = testHelper.indWithNinoYourDetailsMaybe(testUserAnswersIndDetailsSoleTrader).get
        val expectedTitle             = "Your details"
        val expectedKeys: Seq[String] = Seq(
          "What are you registering as?",
          "Is your registered business address in the UK?",
          "Do you have a Unique Taxpayer Reference?",
          "Do you have a National Insurance number?",
          "Your National Insurance number",
          "Your name",
          "Your date of birth"
        )
        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return None when the journey type is not sole trader or individual not connected to a business" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.set(RegistrationTypePage, LLP).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a NINO' is false" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.set(HaveNiNumberPage, false).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a UTR' is true for a Sole Trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.set(HaveUTRPage, true).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a UTR' has not been answered for a Sole Trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.remove(HaveUTRPage).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Is your registered address in the UK' has not been answered for a Sole Trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndDetailsSoleTrader.remove(RegisteredAddressInUkPage).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when Individual registration type has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.remove(RegistrationTypePage).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'Do you have a NINO' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.remove(HaveNiNumberPage).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your NINO' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.remove(NiNumberPage).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your name' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndDetailsSoleTrader.remove(WhatIsYourNameIndividualPage).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your date of birth' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.remove(RegisterDateOfBirthPage).success.value
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return a section when RegistrationType is Individual and all questions have been answered correctly" in new TestData {
        val section: Section          = testHelper.indWithNinoYourDetailsMaybe(testUserAnswersIndDetailsIndividual).get
        val expectedTitle             = "Your details"
        val expectedKeys: Seq[String] = Seq(
          "What are you registering as?",
          "Do you have a National Insurance number?",
          "Your National Insurance number",
          "Your name",
          "Your date of birth"
        )
        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
    }

    "indWithoutIdYourDetailsMaybe" - {
      "must return a section when RegistrationType is SoleTrader, the address is uk or cd and all questions have been answered correctly" in new TestData {
        val section: Section          =
          testHelper.indWithoutIdYourDetailsMaybe(testUserAnswersIndWithoutIdDetailsSoleTrader(true)).get
        val expectedTitle             = "Your details"
        val expectedKeys: Seq[String] = Seq(
          "What are you registering as?",
          "Is your registered business address in the UK?",
          "Do you have a Unique Taxpayer Reference?",
          "Do you have a National Insurance number?",
          "Your name",
          "Your date of birth",
          "Your home address"
        )
        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return a section when RegistrationType is Individual, the address is uk or cd and all questions have been answered correctly" in new TestData {
        val section: Section          =
          testHelper.indWithoutIdYourDetailsMaybe(testUserAnswersIndWithoutIdDetailsIndividual(true)).get
        val expectedTitle             = "Your details"
        val expectedKeys: Seq[String] = Seq(
          "What are you registering as?",
          "Do you have a National Insurance number?",
          "Your name",
          "Your date of birth",
          "Your home address"
        )
        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return a section when RegistrationType is SoleTrader, the address is NOT uk or cd and all questions have been answered correctly" in new TestData {
        val section: Section          =
          testHelper.indWithoutIdYourDetailsMaybe(testUserAnswersIndWithoutIdDetailsSoleTrader(false)).get
        val expectedTitle             = "Your details"
        val expectedKeys: Seq[String] = Seq(
          "What are you registering as?",
          "Is your registered business address in the UK?",
          "Do you have a Unique Taxpayer Reference?",
          "Do you have a National Insurance number?",
          "Your name",
          "Your date of birth",
          "Your home address"
        )
        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return a section when RegistrationType is Individual, the address is NOT uk or cd and all questions have been answered correctly" in new TestData {
        val section: Section          =
          testHelper.indWithoutIdYourDetailsMaybe(testUserAnswersIndWithoutIdDetailsIndividual(false)).get
        val expectedTitle             = "Your details"
        val expectedKeys: Seq[String] = Seq(
          "What are you registering as?",
          "Do you have a National Insurance number?",
          "Your name",
          "Your date of birth",
          "Your home address"
        )
        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return None when the journey type is not sole trader or individual not connected to a business" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).set(RegistrationTypePage, LLP).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a NINO' is true" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsSoleTrader(true).set(HaveNiNumberPage, true).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a UTR' is true for a Sole Trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsSoleTrader(true).set(HaveUTRPage, true).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Is your registered address in the UK' is true for a Sole Trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsSoleTrader(true).set(RegisteredAddressInUkPage, true).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a UTR' has not been answered for a Sole Trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.remove(HaveUTRPage).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Is your registered address in the UK' has not been answered for a Sole Trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsSoleTrader(true).remove(RegisteredAddressInUkPage).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when Individual registration type has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).remove(RegistrationTypePage).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'Do you have a NINO' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).remove(HaveNiNumberPage).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your name' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).remove(IndWithoutNinoNamePage).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your date of birth' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).remove(IndWithoutIdDateOfBirthPage).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your address' (Non uk/cd) page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(false).remove(IndWithoutIdAddressNonUkPage).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your address' (UK/CD) page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).remove(IndWithoutIdAddressInUserAnswers).success.value
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

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
          "Do you have a second contact?"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return a section when all questions have been answered" in new TestData {
        val section: Section          =
          testHelper.getSecondContactDetailsSectionMaybe(testUserAnswersWithSecondContactDetails).get
        val expectedTitle             = "Second contact"
        val expectedKeys: Seq[String] = Seq(
          "Do you have a second contact?",
          "Second contact name",
          "Second contact email address",
          "Can we contact the second contact by phone?",
          "Second contact phone number"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }

      "must return a section when all questions have been answered but can we contact second contact by phone is no" in new TestData {
        val section: Section          =
          testHelper.getSecondContactDetailsSectionMaybe(testUserAnswersWithSecondContactDetailsNoPhoneNumber).get
        val expectedTitle             = "Second contact"
        val expectedKeys: Seq[String] = Seq(
          "Do you have a second contact?",
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
          testUserAnswersWithSecondContactDetails.remove(OrganisationSecondContactPhoneNumberPage).success.value
        )

        section mustBe None
      }
    }
    "indContactDetailsMaybe" - {
      "must return a section when all pages have been answered and user doesn't have a phone number" in new TestData {
        val section: Section          = testHelper.indContactDetailsMaybe(testUserAnswersIndWithoutPhoneNumber).get
        val expectedTitle             = "Contact details"
        val expectedKeys: Seq[String] = Seq(
          "Email address",
          "Can we contact you by phone?"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return a section when all pages have been answered and user has a phone number" in new TestData {
        val section: Section          = testHelper.indContactDetailsMaybe(testUserAnswersIndWithPhoneNumber).get
        val expectedTitle             = "Contact details"
        val expectedKeys: Seq[String] = Seq(
          "Email address",
          "Can we contact you by phone?",
          "Phone number"
        )

        compareRowsAndTitleToExpected(expectedTitle, expectedKeys, section)
      }
      "must return None when user has indicated that they will provide a phone number but don't" in new TestData {
        private val testUserAnswers  = testUserAnswersIndWithPhoneNumber.remove(IndividualPhoneNumberPage).success.value
        val section: Option[Section] = testHelper.indContactDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'Do you have a phone number?' page is not answered" in new TestData {
        private val testUserAnswers  = testUserAnswersIndWithPhoneNumber.remove(IndividualHavePhonePage).success.value
        val section: Option[Section] = testHelper.indContactDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your email?' page is not answered" in new TestData {
        private val testUserAnswers  = testUserAnswersIndWithPhoneNumber.remove(IndividualEmailPage).success.value
        val section: Option[Section] = testHelper.indContactDetailsMaybe(testUserAnswers)

        section mustBe None
      }
    }
  }

  class TestData {
    val testAddress = AddressRegistrationResponse(
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
        IsThisYourBusinessPageDetails(
          businessDetails = BusinessDetails(name = "TEST NAME", address = testAddress),
          pageAnswer = Some(true)
        )
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
      .set(OrganisationSecondContactPhoneNumberPage, "123")
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

    val testUserAnswersOrgWithoutId: UserAnswers = emptyUserAnswers
      .set(RegistrationTypePage, LLP)
      .success
      .value
      .set(RegisteredAddressInUkPage, false)
      .success
      .value
      .set(HaveUTRPage, false)
      .success
      .value
      .set(OrgWithoutIdBusinessNamePage, "Apples and Pears ltd")
      .success
      .value
      .set(
        OrganisationBusinessAddressPage,
        OrganisationBusinessAddress(
          testAddress.addressLine1,
          testAddress.addressLine2,
          townOrCity = "Testton",
          region = testAddress.addressLine4,
          postcode = testAddress.postalCode,
          country = Country("TS", "test", Some("test"))
        )
      )
      .success
      .value
      .set(HaveTradingNamePage, true)
      .success
      .value
      .set(TradingNamePage, "testName")
      .success
      .value

    val testUserAnswersIndDetailsSoleTrader: UserAnswers = emptyUserAnswers
      .set(RegistrationTypePage, SoleTrader)
      .success
      .value
      .set(HaveNiNumberPage, true)
      .success
      .value
      .set(HaveUTRPage, false)
      .success
      .value
      .set(RegisteredAddressInUkPage, false)
      .success
      .value
      .set(NiNumberPage, "123")
      .success
      .value
      .set(WhatIsYourNameIndividualPage, Name("Timmy", "Otthy"))
      .success
      .value
      .set(RegisterDateOfBirthPage, LocalDate.of(2024, 1, 1))
      .success
      .value

    val testUserAnswersIndDetailsIndividual: UserAnswers = emptyUserAnswers
      .set(RegistrationTypePage, Individual)
      .success
      .value
      .set(HaveNiNumberPage, true)
      .success
      .value
      .set(NiNumberPage, "123")
      .success
      .value
      .set(WhatIsYourNameIndividualPage, Name("Timmy", "Otthy"))
      .success
      .value
      .set(RegisterDateOfBirthPage, LocalDate.of(2024, 1, 1))
      .success
      .value

    val testUserAnswersIndWithoutPhoneNumber: UserAnswers = emptyUserAnswers
      .set(IndividualEmailPage, "TEST EMAIL")
      .success
      .value
      .set(IndividualHavePhonePage, false)
      .success
      .value

    val testUserAnswersIndWithPhoneNumber: UserAnswers = emptyUserAnswers
      .set(IndividualEmailPage, "TEST EMAIL")
      .success
      .value
      .set(IndividualHavePhonePage, true)
      .success
      .value
      .set(IndividualPhoneNumberPage, "TEST PHONE NO")
      .success
      .value

    def testUserAnswersIndWithoutIdDetailsSoleTrader(isUkOrCd: Boolean): UserAnswers = {
      val ua = emptyUserAnswers
        .set(RegistrationTypePage, SoleTrader)
        .success
        .value
        .set(HaveNiNumberPage, false)
        .success
        .value
        .set(HaveUTRPage, false)
        .success
        .value
        .set(WhereDoYouLivePage, isUkOrCd)
        .success
        .value
        .set(RegisteredAddressInUkPage, false)
        .success
        .value
        .set(IndWithoutNinoNamePage, Name("Timmy", "Otthy"))
        .success
        .value
        .set(IndWithoutIdDateOfBirthPage, LocalDate.of(2024, 1, 1))
        .success
        .value

      if (isUkOrCd) {
        ua.set(
          IndWithoutIdAddressInUserAnswers,
          AddressUK("L1", Some("L2"), Some("L3"), "C2", "P1", Country("GB", "GB"))
        ).success
          .value
      } else {
        ua.set(
          IndWithoutIdAddressNonUkPage,
          IndWithoutIdAddressNonUk("L1", Some("L2"), "C1", Some("C2"), Some("P1"), Country("GB", "GB"))
        ).success
          .value
      }
    }

    def testUserAnswersIndWithoutIdDetailsIndividual(isUkOrCd: Boolean): UserAnswers =
      testUserAnswersIndWithoutIdDetailsSoleTrader(isUkOrCd)
        .remove(RegisteredAddressInUkPage)
        .success
        .value
        .remove(HaveUTRPage)
        .success
        .value
        .set(RegistrationTypePage, Individual)
        .success
        .value

  }

}
