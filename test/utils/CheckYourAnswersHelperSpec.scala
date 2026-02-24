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
import models.{AddressUk, BusinessDetails, IndWithoutIdAddressNonUk, IsThisYourBusinessPageDetails, Name, OrganisationBusinessAddress, UserAnswers}
import pages.individual.*
import pages.individualWithoutId.{IndWithoutIdAddressNonUkPage, IndWithoutIdDateOfBirthPage, IndWithoutIdUkAddressInUserAnswers, IndWithoutNinoNamePage}
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
          .getOrgWithoutIdDetailsMaybe(testUserAnswersOrgWithoutId.withPage(HaveTradingNamePage, false))
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
        private val testUserAnswers  = testUserAnswersOrgWithoutId.withPage(RegisteredAddressInUkPage, true)
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None if the user says that they have a UTR" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.withPage(HaveUTRPage, true)
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the What are you registering as? has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.withoutPage(RegistrationTypePage)
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the Is your registered address in the UK? has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.withoutPage(RegisteredAddressInUkPage)
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the Do you have a utr has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.withoutPage(HaveUTRPage)
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the org without id business name has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.withoutPage(OrgWithoutIdBusinessNamePage)
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the org without id business address has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.withoutPage(OrganisationBusinessAddressPage)
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the org without id does your business trade under a different name page has not been answered" in new TestData {
        private val testUserAnswers  = testUserAnswersOrgWithoutId.withoutPage(HaveTradingNamePage)
        val section: Option[Section] = testHelper.getOrgWithoutIdDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the user says that they operate under a different trading name, but haven't entered one" in new TestData {
        private val testUserAnswers  =
          testUserAnswersOrgWithoutId.withPage(HaveTradingNamePage, true).withoutPage(TradingNamePage)
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
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withPage(RegistrationTypePage, LLP)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a NINO' is false" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withPage(HaveNiNumberPage, false)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a UTR' is true for a Sole Trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withPage(HaveUTRPage, true)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a UTR' has not been answered for a Sole Trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withoutPage(HaveUTRPage)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Is your registered address in the UK' has not been answered for a Sole Trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndDetailsSoleTrader.withoutPage(RegisteredAddressInUkPage)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when Individual registration type has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withoutPage(RegistrationTypePage)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'Do you have a NINO' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withoutPage(HaveNiNumberPage)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your NINO' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withoutPage(NiNumberPage)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your name' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndDetailsSoleTrader.withoutPage(WhatIsYourNameIndividualPage)
        val section: Option[Section] = testHelper.indWithNinoYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your date of birth' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withoutPage(RegisterDateOfBirthPage)
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
          testUserAnswersIndWithoutIdDetailsIndividual(true).withPage(RegistrationTypePage, LLP)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a NINO' is true" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsSoleTrader(true).withPage(HaveNiNumberPage, true)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a UTR' is true for a Sole Trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsSoleTrader(true).withPage(HaveUTRPage, true)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Is your registered address in the UK' is true for a Sole Trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsSoleTrader(true).withPage(RegisteredAddressInUkPage, true)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Do you have a UTR' has not been answered for a Sole Trader" in new TestData {
        private val testUserAnswers  = testUserAnswersIndDetailsSoleTrader.withoutPage(HaveUTRPage)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when the 'Is your registered address in the UK' has not been answered for a Sole Trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsSoleTrader(true).withoutPage(RegisteredAddressInUkPage)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when Individual registration type has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).withoutPage(RegistrationTypePage)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'Do you have a NINO' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).withoutPage(HaveNiNumberPage)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your name' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).withoutPage(IndWithoutNinoNamePage)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your date of birth' page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).withoutPage(IndWithoutIdDateOfBirthPage)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your address' (Non uk/cd) page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(false).withoutPage(IndWithoutIdAddressNonUkPage)
        val section: Option[Section] = testHelper.indWithoutIdYourDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your address' (UK/CD) page has not been answered for an individual or sole trader" in new TestData {
        private val testUserAnswers  =
          testUserAnswersIndWithoutIdDetailsIndividual(true).withoutPage(IndWithoutIdUkAddressInUserAnswers)
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
          testUserAnswersWithFirstContactDetails.withoutPage(FirstContactNamePage)
        )

        section mustBe None
      }

      "must return None when first contact email is not answered" in new TestData {
        val section: Option[Section] = testHelper.getFirstContactDetailsSectionMaybe(
          testUserAnswersWithFirstContactDetails.withoutPage(FirstContactEmailPage)
        )

        section mustBe None
      }

      "must return None when can we contact you by phone is not answered" in new TestData {
        val section: Option[Section] = testHelper.getFirstContactDetailsSectionMaybe(
          testUserAnswersWithFirstContactDetails.withoutPage(FirstContactPhonePage)
        )

        section mustBe None
      }

      "must return None when can we contact you by phone is yes but there is no phone number" in new TestData {
        val section: Option[Section] = testHelper.getFirstContactDetailsSectionMaybe(
          testUserAnswersWithFirstContactDetails.withoutPage(FirstContactPhoneNumberPage)
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
          testUserAnswersWithSecondContactDetails.withoutPage(OrganisationSecondContactNamePage)
        )

        section mustBe None
      }

      "must return None when can we contact second contact you by phone is not answered" in new TestData {
        val section: Option[Section] = testHelper.getSecondContactDetailsSectionMaybe(
          testUserAnswersWithSecondContactDetails.withoutPage(OrganisationSecondContactEmailPage)
        )

        section mustBe None
      }

      "must return None when can we contact you by phone is yes but there is no phone number" in new TestData {
        val section: Option[Section] = testHelper.getSecondContactDetailsSectionMaybe(
          testUserAnswersWithSecondContactDetails.withoutPage(OrganisationSecondContactPhoneNumberPage)
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
        private val testUserAnswers  = testUserAnswersIndWithPhoneNumber.withoutPage(IndividualPhoneNumberPage)
        val section: Option[Section] = testHelper.indContactDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'Do you have a phone number?' page is not answered" in new TestData {
        private val testUserAnswers  = testUserAnswersIndWithPhoneNumber.withoutPage(IndividualHavePhonePage)
        val section: Option[Section] = testHelper.indContactDetailsMaybe(testUserAnswers)

        section mustBe None
      }
      "must return None when 'What is your email?' page is not answered" in new TestData {
        private val testUserAnswers  = testUserAnswersIndWithPhoneNumber.withoutPage(IndividualEmailPage)
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
      .withPage(
        IsThisYourBusinessPage,
        IsThisYourBusinessPageDetails(
          businessDetails = BusinessDetails(name = "TEST NAME", address = testAddress, safeId = testSafeId),
          pageAnswer = Some(true)
        )
      )

    val testUserAnswersWithFirstContactDetails: UserAnswers = emptyUserAnswers
      .withPage(FirstContactNamePage, "TEST NAME")
      .withPage(FirstContactEmailPage, "TEST EMAIL")
      .withPage(FirstContactPhonePage, true)
      .withPage(FirstContactPhoneNumberPage, "123")

    val testUserAnswersWithFirstContactDetailsNoPhoneNumber: UserAnswers = emptyUserAnswers
      .withPage(FirstContactNamePage, "TEST NAME")
      .withPage(FirstContactEmailPage, "TEST EMAIL")
      .withPage(FirstContactPhonePage, false)

    val testUserAnswersWithSecondContactDetailsAsNo: UserAnswers = emptyUserAnswers
      .withPage(OrganisationHaveSecondContactPage, false)

    val testUserAnswersWithSecondContactDetails: UserAnswers = emptyUserAnswers
      .withPage(OrganisationHaveSecondContactPage, true)
      .withPage(OrganisationSecondContactNamePage, "TEST NAME")
      .withPage(OrganisationSecondContactEmailPage, "TEST EMAIL")
      .withPage(OrganisationSecondContactHavePhonePage, true)
      .withPage(OrganisationSecondContactPhoneNumberPage, "123")

    val testUserAnswersWithSecondContactDetailsNoPhoneNumber: UserAnswers = emptyUserAnswers
      .withPage(OrganisationHaveSecondContactPage, true)
      .withPage(OrganisationSecondContactNamePage, "TEST NAME")
      .withPage(OrganisationSecondContactEmailPage, "TEST EMAIL")
      .withPage(OrganisationSecondContactHavePhonePage, false)

    val testUserAnswersOrgWithoutId: UserAnswers = emptyUserAnswers
      .withPage(RegistrationTypePage, LLP)
      .withPage(RegisteredAddressInUkPage, false)
      .withPage(HaveUTRPage, false)
      .withPage(OrgWithoutIdBusinessNamePage, "Apples and Pears ltd")
      .withPage(
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
      .withPage(HaveTradingNamePage, true)
      .withPage(TradingNamePage, "testName")

    val testUserAnswersIndDetailsSoleTrader: UserAnswers = emptyUserAnswers
      .withPage(RegistrationTypePage, SoleTrader)
      .withPage(HaveNiNumberPage, true)
      .withPage(HaveUTRPage, false)
      .withPage(RegisteredAddressInUkPage, false)
      .withPage(NiNumberPage, "123")
      .withPage(WhatIsYourNameIndividualPage, Name("Timmy", "Otthy"))
      .withPage(RegisterDateOfBirthPage, LocalDate.of(2024, 1, 1))

    val testUserAnswersIndDetailsIndividual: UserAnswers = emptyUserAnswers
      .withPage(RegistrationTypePage, Individual)
      .withPage(HaveNiNumberPage, true)
      .withPage(NiNumberPage, "123")
      .withPage(WhatIsYourNameIndividualPage, Name("Timmy", "Otthy"))
      .withPage(RegisterDateOfBirthPage, LocalDate.of(2024, 1, 1))

    val testUserAnswersIndWithoutPhoneNumber: UserAnswers = emptyUserAnswers
      .withPage(IndividualEmailPage, "TEST EMAIL")
      .withPage(IndividualHavePhonePage, false)

    val testUserAnswersIndWithPhoneNumber: UserAnswers = emptyUserAnswers
      .withPage(IndividualEmailPage, "TEST EMAIL")
      .withPage(IndividualHavePhonePage, true)
      .withPage(IndividualPhoneNumberPage, "TEST PHONE NO")

    def testUserAnswersIndWithoutIdDetailsSoleTrader(isUkOrCd: Boolean): UserAnswers = {
      val ua = emptyUserAnswers
        .withPage(RegistrationTypePage, SoleTrader)
        .withPage(HaveNiNumberPage, false)
        .withPage(HaveUTRPage, false)
        .withPage(WhereDoYouLivePage, isUkOrCd)
        .withPage(RegisteredAddressInUkPage, false)
        .withPage(IndWithoutNinoNamePage, Name("Timmy", "Otthy"))
        .withPage(IndWithoutIdDateOfBirthPage, LocalDate.of(2024, 1, 1))

      if (isUkOrCd) {
        ua.withPage(
          IndWithoutIdUkAddressInUserAnswers,
          testAddressUk
        )

      } else {
        ua.withPage(
          IndWithoutIdAddressNonUkPage,
          IndWithoutIdAddressNonUk("L1", Some("L2"), "C1", Some("C2"), Some("P1"), Country("GB", "GB"))
        )

      }
    }

    def testUserAnswersIndWithoutIdDetailsIndividual(isUkOrCd: Boolean): UserAnswers =
      testUserAnswersIndWithoutIdDetailsSoleTrader(isUkOrCd)
        .withoutPage(RegisteredAddressInUkPage)
        .withoutPage(HaveUTRPage)
        .withPage(RegistrationTypePage, Individual)

  }

}
