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

class IndividualRegistrationTypePageForNavigatorAndCleanupSpec extends SpecBase {
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

  "IndividualRegistrationTypePageForNavigatorAndCleanup" - {
    "cleanup" - {
      "must clear answers" - {
        "when the new answer is different to the previous one and is sole trader" in {
          val ua     = generateUserAnswers(SoleTrader)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, SoleTrader)
          val result =
            IndividualRegistrationTypePageForNavigatorAndCleanup
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
          val ua     = generateUserAnswers(Individual)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, Individual)
          val result =
            IndividualRegistrationTypePageForNavigatorAndCleanup
              .cleanup(newValue = Individual, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.get(RegisteredAddressInUkPage)            mustBe empty
          result.get(HaveUTRPage)                          mustBe empty
          result.get(UniqueTaxpayerReferenceInUserAnswers) mustBe empty
          result.get(WhatIsYourNamePage)                   mustBe empty
          result.get(IsThisYourBusinessPage)               mustBe empty
        }

        "when answer has not changed, do nothing" in {
          val ua     = generateUserAnswers(Individual)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, Individual)
          val result = IndividualRegistrationTypePageForNavigatorAndCleanup
            .cleanup(newValue = Individual, updatedUserAnswers = ua, hasChanged = false)
            .success
            .value

          result mustBe ua
        }
      }

      "match flag must" - {
        "remain true and safe id not cleared when the new answer is different to the previous one and is sole trader" in {
          val ua = generateUserAnswers(SoleTrader)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, SoleTrader)
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))

          val result =
            IndividualRegistrationTypePageForNavigatorAndCleanup
              .cleanup(newValue = SoleTrader, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe true
          result.safeId        mustBe Some(SafeId(testSafeId))
        }

        "remain false and keep safe id clear when the new answer is different to the previous one and is sole trader" in {
          val ua = generateUserAnswers(SoleTrader)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, SoleTrader)
            .copy(hasValidMatch = false, safeId = None)

          val result =
            IndividualRegistrationTypePageForNavigatorAndCleanup
              .cleanup(newValue = SoleTrader, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }

        "remain true and safe id not cleared when the new answer is different to the previous one and is individual not connected to a business" in {
          val ua = generateUserAnswers(Individual)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, Individual)
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))

          val result =
            IndividualRegistrationTypePageForNavigatorAndCleanup
              .cleanup(newValue = Individual, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe true
          result.safeId        mustBe Some(SafeId(testSafeId))
        }

        "remain false and keep safe id clear when the new answer is different to the previous one and is individual not connected to a business" in {
          val ua = generateUserAnswers(SoleTrader)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, SoleTrader)
            .copy(hasValidMatch = false, safeId = None)

          val result =
            IndividualRegistrationTypePageForNavigatorAndCleanup
              .cleanup(newValue = SoleTrader, updatedUserAnswers = ua, hasChanged = true)
              .success
              .value

          result.hasValidMatch mustBe false
          result.safeId        mustBe None
        }

        "remain unchanged and safe id kept when answer has not changed, and hasValidMatch is true" in {
          val ua = generateUserAnswers(SoleTrader)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, SoleTrader)
            .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))

          val result =
            IndividualRegistrationTypePageForNavigatorAndCleanup
              .cleanup(newValue = SoleTrader, updatedUserAnswers = ua, hasChanged = false)
              .success
              .value

          result.hasValidMatch mustBe true
          result.safeId        mustBe Some(SafeId(testSafeId))
        }

        "remain unchanged and safe id kept when answer has not changed, and hasValidMatch is false" in {
          val ua = generateUserAnswers(Individual)
            .withPage(IndividualRegistrationTypePageForNavigatorAndCleanup, Individual)
            .copy(hasValidMatch = false, safeId = None)

          val result =
            IndividualRegistrationTypePageForNavigatorAndCleanup
              .cleanup(newValue = Individual, updatedUserAnswers = ua, hasChanged = false)
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
      case _          => fail("Registration type can only be SoleTrader or Individual")
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
