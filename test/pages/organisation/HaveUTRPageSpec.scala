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

package pages.organisation

import base.SpecBase
import models.{AddressUk, IndFindAddress, IndWithoutIdAddressNonUk, IsThisYourBusinessPageDetails, Name, OrganisationBusinessAddress, SafeId, UniqueTaxpayerReference, UserAnswers}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import pages.IsThisYourBusinessPage
import pages.individual.*
import pages.individualWithoutId.*
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}

import java.time.LocalDate

class HaveUTRPageSpec extends SpecBase {
  private val testParamGenerator = for {
    stringField     <- Gen.alphaStr.suchThat(_.nonEmpty)
    booleanField    <- Gen.oneOf(true, false)
    dob             <- Gen.choose(LocalDate.of(1901, 1, 1), LocalDate.now)
    businessAddress <- arbitrary[OrganisationBusinessAddress]
    name            <- arbitrary[Name]
    findAddress     <- arbitrary[IndFindAddress]
    addressNonUk    <- arbitrary[IndWithoutIdAddressNonUk]
    addressUk       <- arbitrary[AddressUk]
    utr             <- arbitrary[UniqueTaxpayerReference]
    itybpd          <- arbitrary[IsThisYourBusinessPageDetails]
    addressLookup   <- arbitrary[Seq[AddressUk]]
  } yield (
    stringField,
    booleanField,
    dob,
    businessAddress,
    name,
    findAddress,
    addressNonUk,
    addressUk,
    utr,
    itybpd,
    addressLookup
  )

  "HaveUTRPage" - {
    "cleanup" - {
      "must clear answers" - {
        "when the answer is true, and is different to the previous one" in {
          val ua     = generateUserAnswers(true)
          val result = HaveUTRPage.cleanup(newValue = true, updatedUserAnswers = ua, hasChanged = true).success.value

          result.get(HaveNiNumberPage)                      mustBe empty
          result.get(HaveTradingNamePage)                   mustBe empty
          result.get(OrganisationBusinessAddressPage)       mustBe empty
          result.get(OrgWithoutIdBusinessNamePage)          mustBe empty
          result.get(TradingNamePage)                       mustBe empty
          result.get(NiNumberPage)                          mustBe empty
          result.get(WhatIsYourNameIndividualPage)          mustBe empty
          result.get(RegisterDateOfBirthPage)               mustBe empty
          result.get(IndFindAddressAdditionalCallUa)        mustBe empty
          result.get(IndFindAddressPage)                    mustBe empty
          result.get(WhereDoYouLivePage)                    mustBe empty
          result.get(AddressLookupPage)                     mustBe empty
          result.get(IndWithoutNinoNamePage)                mustBe empty
          result.get(IndWithoutIdAddressNonUkPage)          mustBe empty
          result.get(IndWithoutIdAddressPagePrePop)         mustBe empty
          result.get(IndWithoutIdChooseAddressPage)         mustBe empty
          result.get(IndWithoutIdDateOfBirthPage)           mustBe empty
          result.get(IndWithoutIdSelectedChooseAddressPage) mustBe empty
          result.get(IndWithoutIdUkAddressInUserAnswers)    mustBe empty
        }

        "when the answer is false, and is different to the previous one" in {
          val ua     = generateUserAnswers(false)
          val result = HaveUTRPage.cleanup(newValue = false, updatedUserAnswers = ua, hasChanged = true).success.value

          result.get(UniqueTaxpayerReferenceInUserAnswers) mustBe empty
          result.get(WhatIsYourNamePage)                   mustBe empty
          result.get(IsThisYourBusinessPage)               mustBe empty
          result.get(WhatIsTheNameOfYourBusinessPage)      mustBe empty
        }

        "when answer has not changed, and new answer is true, do nothing" in {
          val ua     = emptyUserAnswers
          val result = HaveUTRPage
            .cleanup(newValue = true, updatedUserAnswers = ua, hasChanged = true)
            .success
            .value

          result mustBe ua
        }

        "when answer has not changed, and new answer is false, do nothing" in {
          val ua     = emptyUserAnswers
          val result = HaveUTRPage
            .cleanup(newValue = true, updatedUserAnswers = ua, hasChanged = false)
            .success
            .value

          result mustBe ua
        }
      }

      "must clear match flag and remove safe id when the answer has changed to yes" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result = HaveUTRPage.cleanup(newValue = true, updatedUserAnswers = ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must clear match flag and remove safe id when the answer has changed to no" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result = HaveUTRPage.cleanup(newValue = false, updatedUserAnswers = ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep match flag false and safe id NOT present when the answer has changed to yes" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result = HaveUTRPage.cleanup(newValue = true, updatedUserAnswers = ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep match flag false and safe id NOT present when the answer has changed to no" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result = HaveUTRPage.cleanup(newValue = false, updatedUserAnswers = ua, hasChanged = true).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
      "must keep the flag as true and safe id as present when the answer has not changed" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))
        val result = HaveUTRPage.cleanup(newValue = true, updatedUserAnswers = ua, hasChanged = false).success.value

        result.hasValidMatch mustBe true
        result.safeId        mustBe Some(SafeId(testSafeId))
      }
      "must keep the flag as false and safe id as NOt present when the answer has not changed" in {
        val ua     = emptyUserAnswers.copy(hasValidMatch = false, safeId = None)
        val result = HaveUTRPage.cleanup(newValue = true, updatedUserAnswers = ua, hasChanged = false).success.value

        result.hasValidMatch mustBe false
        result.safeId        mustBe None
      }
    }
  }

  def generateUserAnswers(withUtrPages: Boolean): UserAnswers = {
    val answers =
      if (withUtrPages) { createUserAnswersForWithoutUtrCleanup }
      else { createUserAnswersForWithUtrCleanup }

    answers.sample match {
      case Some(value) => value
      case None        => generateUserAnswers(withUtrPages) // retry if None
    }
  }

  def createUserAnswersForWithoutUtrCleanup: Gen[UserAnswers] =
    for {
      (
        stringField,
        booleanField,
        dob,
        businessAddress,
        name,
        findAddress,
        addressNonUk,
        addressUk,
        utr,
        itybpd,
        addressLookup
      ) <-
        testParamGenerator.suchThat(_ != null)
    } yield emptyUserAnswers
      .withPage(HaveNiNumberPage, booleanField)
      .withPage(HaveTradingNamePage, booleanField)
      .withPage(OrganisationBusinessAddressPage, businessAddress)
      .withPage(OrgWithoutIdBusinessNamePage, stringField)
      .withPage(TradingNamePage, stringField)
      .withPage(NiNumberPage, stringField)
      .withPage(WhatIsYourNameIndividualPage, name)
      .withPage(RegisterDateOfBirthPage, dob)
      .withPage(IndFindAddressAdditionalCallUa, booleanField)
      .withPage(IndFindAddressPage, findAddress)
      .withPage(WhereDoYouLivePage, booleanField)
      .withPage(AddressLookupPage, addressLookup)
      .withPage(IndWithoutNinoNamePage, name)
      .withPage(IndWithoutIdAddressNonUkPage, addressNonUk)
      .withPage(IndWithoutIdAddressPagePrePop, addressUk)
      .withPage(IndWithoutIdChooseAddressPage, stringField)
      .withPage(IndWithoutIdDateOfBirthPage, dob)
      .withPage(IndWithoutIdSelectedChooseAddressPage, addressUk)
      .withPage(IndWithoutIdUkAddressInUserAnswers, addressUk)

  def createUserAnswersForWithUtrCleanup: Gen[UserAnswers] =
    for {
      (
        stringField,
        booleanField,
        dob,
        businessAddress,
        name,
        findAddress,
        addressNonUk,
        addressUk,
        utr,
        itybpd,
        addressLookup
      ) <-
        testParamGenerator.suchThat(_ != null)
    } yield emptyUserAnswers
      .withPage(UniqueTaxpayerReferenceInUserAnswers, utr)
      .withPage(WhatIsYourNamePage, name)
      .withPage(WhatIsTheNameOfYourBusinessPage, stringField)
      .withPage(IsThisYourBusinessPage, itybpd)

}
