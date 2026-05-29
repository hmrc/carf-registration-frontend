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

package pages

import base.SpecBase
import models.*
import models.RegistrationType.{Individual, LimitedCompany, SoleTrader}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.individual.*
import pages.individualWithoutId.*
import pages.orgWithoutId.*
import pages.organisation.*

import java.time.LocalDate

class RegistrationTypePageSpec extends SpecBase {
  private val testParamGenerator = for {
    stringField     <- Gen.alphaStr.suchThat(_.nonEmpty)
    booleanField    <- Gen.oneOf(true, false)
    postcode        <- Gen.alphaNumStr.suchThat(_.nonEmpty)
    dob             <- Gen.choose(LocalDate.of(1901, 1, 1), LocalDate.now)
    businessAddress <- arbitrary[OrganisationBusinessAddress]
    name            <- arbitrary[Name]
    findAddress     <- arbitrary[IndFindAddress]
    addressNonUk    <- arbitrary[IndWithoutIdAddressNonUk]
    addressUk       <- arbitrary[AddressUk]
    utr             <- arbitrary[UniqueTaxpayerReference]
    itybpd          <- arbitrary[IsThisYourBusinessPageDetails]
    addressLookup   <- arbitrary[Seq[AddressAndUPRN]]
    longField       <- Gen.long
  } yield (
    stringField,
    booleanField,
    postcode,
    dob,
    businessAddress,
    name,
    findAddress,
    addressNonUk,
    addressUk,
    utr,
    itybpd,
    addressLookup,
    longField
  )

  "RegistrationTypePage" - {
    "cleanup" - {
      "must clear answers" - {
        "when the new answer is different to the previous one and is sole trader" in {
          val ua     = generateUserAnswers(SoleTrader).withPage(RegistrationTypePage, SoleTrader)
          val result =
            RegistrationTypePage
              .cleanup(newValue = SoleTrader, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.get(WhatIsTheNameOfYourBusinessPage)          mustBe empty
          result.get(FirstContactNamePage)                     mustBe empty
          result.get(FirstContactEmailPage)                    mustBe empty
          result.get(FirstContactPhonePage)                    mustBe empty
          result.get(FirstContactPhoneNumberPage)              mustBe empty
          result.get(OrganisationHaveSecondContactPage)        mustBe empty
          result.get(OrganisationSecondContactNamePage)        mustBe empty
          result.get(OrganisationSecondContactEmailPage)       mustBe empty
          result.get(OrganisationSecondContactHavePhonePage)   mustBe empty
          result.get(OrganisationSecondContactPhoneNumberPage) mustBe empty
          result.get(HaveTradingNamePage)                      mustBe empty
          result.get(TradingNamePage)                          mustBe empty
          result.get(OrgWithoutIdBusinessNamePage)             mustBe empty
          result.get(OrganisationBusinessAddressPage)          mustBe empty
        }

        "when the new answer is different to the previous one and is individual not connected to a business" in {
          val ua     = generateUserAnswers(Individual).withPage(RegistrationTypePage, Individual)
          val result =
            RegistrationTypePage
              .cleanup(newValue = Individual, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.get(RegisteredAddressInUkPage)            mustBe empty
          result.get(HaveUTRPage)                          mustBe empty
          result.get(UniqueTaxpayerReferenceInUserAnswers) mustBe empty
          result.get(WhatIsYourNamePage)                   mustBe empty
          result.get(IsThisYourBusinessPage)               mustBe empty
        }

        "when the new answer is different to the previous one and is Limited Company (non sole trader)" in {
          val ua     = generateUserAnswers(LimitedCompany).withPage(RegistrationTypePage, LimitedCompany)
          val result = RegistrationTypePage
            .cleanup(newValue = LimitedCompany, updatedUserAnswers = ua, hasChanged = true)
            .success
            .value

          result.get(WhatIsYourNamePage)                    mustBe empty
          result.get(HaveNiNumberPage)                      mustBe empty
          result.get(NiNumberPage)                          mustBe empty
          result.get(WhatIsYourNameIndividualPage)          mustBe empty
          result.get(RegisterDateOfBirthPage)               mustBe empty
          result.get(IndFindAddressAdditionalCallUa)        mustBe empty
          result.get(IndFindAddressPage)                    mustBe empty
          result.get(WhereDoYouLivePage)                    mustBe empty
          result.get(AddressLookupPage)                     mustBe empty
          result.get(AddressUPRNUserAnswers)                mustBe empty
          result.get(IndWithoutNinoNamePage)                mustBe empty
          result.get(IndWithoutIdAddressNonUkPage)          mustBe empty
          result.get(IndWithoutIdAddressPagePrePop)         mustBe empty
          result.get(IndWithoutIdChooseAddressPage)         mustBe empty
          result.get(IndWithoutIdDateOfBirthPage)           mustBe empty
          result.get(IndWithoutIdSelectedChooseAddressPage) mustBe empty
          result.get(IndWithoutIdUkAddressInUserAnswers)    mustBe empty
          result.get(IndividualEmailPage)                   mustBe empty
          result.get(IndividualHavePhonePage)               mustBe empty
          result.get(IndividualPhoneNumberPage)             mustBe empty
        }

        "when answer has not changed, do nothing" in {
          val ua     = emptyUserAnswers
          val result = RegistrationTypePage
            .cleanup(newValue = LimitedCompany, updatedUserAnswers = ua, hasChanged = false)
            .success
            .value

          result mustBe ua
        }
      }

      "match flag must be" - {
        "switched to false and safe id cleared when the new answer is different to the previous one and is sole trader" in {
          val ua = generateUserAnswers(SoleTrader)
            .withPage(RegistrationTypePage, SoleTrader)
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))

          val result =
            RegistrationTypePage
              .cleanup(newValue = SoleTrader, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }

        "remain false and keep safe id clear when the new answer is different to the previous one and is sole trader" in {
          val ua = generateUserAnswers(SoleTrader)
            .withPage(RegistrationTypePage, SoleTrader)
            .copy(hasValidMatch = false, safeId = None)

          val result =
            RegistrationTypePage
              .cleanup(newValue = SoleTrader, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }

        "switched to false and safe id cleared when the new answer is different to the previous one and is individual not connected to a business" in {
          val ua = generateUserAnswers(Individual)
            .withPage(RegistrationTypePage, Individual)
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))

          val result =
            RegistrationTypePage
              .cleanup(newValue = Individual, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }

        "remain false and keep safe id clear when the new answer is different to the previous one and is individual not connected to a business" in {
          val ua = generateUserAnswers(SoleTrader)
            .withPage(RegistrationTypePage, SoleTrader)
            .copy(hasValidMatch = false, safeId = None)

          val result =
            RegistrationTypePage
              .cleanup(newValue = SoleTrader, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }

        "switched to false and safe id cleared when the new answer is different to the previous one and is Limited Company (non sole trader)" in {
          val ua = generateUserAnswers(LimitedCompany)
            .withPage(RegistrationTypePage, LimitedCompany)
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))

          val result =
            RegistrationTypePage
              .cleanup(newValue = LimitedCompany, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }

        "remain false and keep safe id clear when the new answer is different to the previous one and is not Limited Company (non sole trader)" in {
          val ua = generateUserAnswers(LimitedCompany)
            .withPage(RegistrationTypePage, LimitedCompany)
            .copy(hasValidMatch = false, safeId = None)

          val result =
            RegistrationTypePage
              .cleanup(newValue = LimitedCompany, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }

        "not changed and safe id kept when answer has not changed, and hasValidMatch is true" in {
          val ua = generateUserAnswers(LimitedCompany)
            .withPage(RegistrationTypePage, LimitedCompany)
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))

          val result =
            RegistrationTypePage
              .cleanup(newValue = LimitedCompany, updatedUserAnswers = ua, hasChanged = false)
              .success
              .value

          result.hasValidMatch mustBe true
          result.safeId        mustBe Some(SafeId(testSafeId))
        }

        "not changed and safe id kept when answer has not changed, and hasValidMatch is false" in {
          val ua = generateUserAnswers(LimitedCompany)
            .withPage(RegistrationTypePage, LimitedCompany)
            .copy(hasValidMatch = false, safeId = None)

          val result =
            RegistrationTypePage
              .cleanup(newValue = LimitedCompany, updatedUserAnswers = ua, hasChanged = false)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }
      }
    }
  }

  def generateUserAnswers(cleanupType: RegistrationType): UserAnswers = {
    val answers = cleanupType match {
      case SoleTrader => createUserAnswersForNonSoleTraderCleanup.suchThat(_ != null)
      case Individual => createUserAnswersForIndividualCleanup.suchThat(_ != null)
      case _          => createUserAnswersForSoleTraderCleanup.suchThat(_ != null)
    }
    answers.sample match {
      case Some(value) => value
      case None        => generateUserAnswers(cleanupType) // retry if None
    }
  }

  def createUserAnswersForNonSoleTraderCleanup: Gen[UserAnswers] =
    for {
      (
        stringField,
        booleanField,
        postcode,
        dob,
        businessAddress,
        name,
        findAddress,
        addressNonUk,
        addressUk,
        utr,
        itybpd,
        addressesAndUPRNs,
        longField
      ) <-
        testParamGenerator.suchThat(_ != null)
    } yield emptyUserAnswers
      .withPage(WhatIsTheNameOfYourBusinessPage, stringField)
      .withPage(IsThisYourBusinessPage, itybpd)
      .withPage(FirstContactNamePage, stringField)
      .withPage(FirstContactEmailPage, stringField)
      .withPage(FirstContactPhonePage, booleanField)
      .withPage(FirstContactPhoneNumberPage, stringField)
      .withPage(OrganisationHaveSecondContactPage, booleanField)
      .withPage(OrganisationSecondContactNamePage, stringField)
      .withPage(OrganisationSecondContactEmailPage, stringField)
      .withPage(OrganisationSecondContactHavePhonePage, booleanField)
      .withPage(OrganisationSecondContactPhoneNumberPage, stringField)
      .withPage(HaveTradingNamePage, booleanField)
      .withPage(TradingNamePage, stringField)
      .withPage(OrgWithoutIdBusinessNamePage, stringField)
      .withPage(OrganisationBusinessAddressPage, businessAddress)

  def createUserAnswersForSoleTraderCleanup: Gen[UserAnswers] =
    for {
      (
        stringField,
        booleanField,
        postcode,
        dob,
        businessAddress,
        name,
        findAddress,
        addressNonUk,
        addressUk,
        utr,
        itybpd,
        addressesAndUPRNs,
        longField
      ) <-
        testParamGenerator.suchThat(_ != null)
    } yield emptyUserAnswers
      .withPage(WhatIsYourNamePage, name)
      .withPage(IsThisYourBusinessPage, itybpd)
      .withPage(HaveNiNumberPage, booleanField)
      .withPage(NiNumberPage, stringField)
      .withPage(WhatIsYourNameIndividualPage, name)
      .withPage(RegisterDateOfBirthPage, dob)
      .withPage(IndFindAddressAdditionalCallUa, booleanField)
      .withPage(IndFindAddressPage, findAddress)
      .withPage(WhereDoYouLivePage, booleanField)
      .withPage(AddressLookupPage, addressesAndUPRNs)
      .withPage(AddressUPRNUserAnswers, longField)
      .withPage(IndWithoutNinoNamePage, name)
      .withPage(IndWithoutIdAddressNonUkPage, addressNonUk)
      .withPage(IndWithoutIdAddressPagePrePop, addressUk)
      .withPage(IndWithoutIdChooseAddressPage, stringField)
      .withPage(IndWithoutIdDateOfBirthPage, dob)
      .withPage(IndWithoutIdSelectedChooseAddressPage, addressUk)
      .withPage(IndWithoutIdUkAddressInUserAnswers, addressUk)
      .withPage(IndividualEmailPage, stringField)
      .withPage(IndividualHavePhonePage, booleanField)
      .withPage(IndividualPhoneNumberPage, stringField)

  def createUserAnswersForIndividualCleanup: Gen[UserAnswers] =
    for {
      (
        stringField,
        booleanField,
        postcode,
        dob,
        businessAddress,
        name,
        findAddress,
        addressNonUk,
        addressUk,
        utr,
        itybpd,
        addressesAndUPRNs,
        longField
      ) <-
        testParamGenerator.suchThat(_ != null)
    } yield emptyUserAnswers
      .withPage(RegisteredAddressInUkPage, booleanField)
      .withPage(HaveUTRPage, booleanField)
      .withPage(UniqueTaxpayerReferenceInUserAnswers, utr)
      .withPage(WhatIsYourNamePage, name)
      .withPage(IsThisYourBusinessPage, itybpd)

}
