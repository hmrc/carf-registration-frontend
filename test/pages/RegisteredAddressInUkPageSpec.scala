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
import models.RegistrationType.{Individual, LLP, SoleTrader}
import models.{IndFindAddress, Name, SafeId, UserAnswers}
import pages.individual.*
import pages.individualWithoutId.*
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}
import pages.organisation.{FirstContactEmailPage, FirstContactNamePage, HaveUTRPage}

import java.time.LocalDate

class RegisteredAddressInUkPageSpec extends SpecBase {

  private val firstContactName         = "Don Joe"
  private val firstContactEmail        = "firstcontact@carf.com"
  private val individualContactEmail   = "individual@carf.com"
  private val individualContactPhone   = "07475376374"
  private val organisationBusinessName = "OrgName"
  private val organisationTradingName  = "TradingName"
  private val nino                     = "AA123456A"

  private def baseAnswersWithValidatedMatch(registeredAddressInUk: Boolean): UserAnswers = emptyUserAnswers
    .withPage(RegisteredAddressInUkPage, registeredAddressInUk)
    .withPage(RegistrationTypePage, LLP)
    .withPage(FirstContactNamePage, firstContactName)
    .withPage(FirstContactEmailPage, firstContactEmail)
    .withPage(IndividualEmailPage, individualContactEmail)
    .withPage(IndividualHavePhonePage, true)
    .withPage(IndividualPhoneNumberPage, individualContactPhone)
    .copy(hasValidMatch = true, safeId = Some(SafeId(testSafeId)))

  private def withOrganisationWithoutIdAnswers = baseAnswersWithValidatedMatch(registeredAddressInUk = false)
    .withPage(HaveUTRPage, false)
    .withPage(RegistrationTypePage, LLP)
    .withPage(OrgWithoutIdBusinessNamePage, organisationBusinessName)
    .withPage(HaveTradingNamePage, true)
    .withPage(TradingNamePage, organisationTradingName)
    .withPage(OrganisationBusinessAddressPage, testBusinessAddress)

  private def withSoleTraderWithNinoAnswers = baseAnswersWithValidatedMatch(registeredAddressInUk = false)
    .withPage(HaveUTRPage, false)
    .withPage(RegistrationTypePage, SoleTrader)
    .withPage(HaveNiNumberPage, true)
    .withPage(NiNumberPage, nino)
    .withPage(WhatIsYourNameIndividualPage, Name("Sole", "Trader"))
    .withPage(RegisterDateOfBirthPage, LocalDate.of(1990, 1, 1))

  private def withIndividualWithNinoAnswers = baseAnswersWithValidatedMatch(registeredAddressInUk = false)
    .withPage(HaveUTRPage, false)
    .withPage(RegistrationTypePage, Individual)
    .withPage(HaveNiNumberPage, true)
    .withPage(NiNumberPage, nino)
    .withPage(WhatIsYourNameIndividualPage, Name("Individual", "Nino"))
    .withPage(RegisterDateOfBirthPage, LocalDate.of(1990, 1, 1))

  private def withSoleTraderWithoutIdAnswers = baseAnswersWithValidatedMatch(registeredAddressInUk = false)
    .withPage(HaveUTRPage, false)
    .withPage(RegistrationTypePage, SoleTrader)
    .withPage(HaveNiNumberPage, false)
    .withPage(IndWithoutNinoNamePage, Name("Sole", "WithoutId"))
    .withPage(IndWithoutIdDateOfBirthPage, LocalDate.of(1991, 2, 2))
    .withPage(WhereDoYouLivePage, true)
    .withPage(IndFindAddressAdditionalCallUa, true)
    .withPage(IndFindAddressPage, IndFindAddress("AB12CD", Some("1")))
    .withPage(IndWithoutIdAddressPagePrePop, testAddressUk)
    .withPage(IndWithoutIdChooseAddressPage, "1")
    .withPage(IndWithoutIdSelectedChooseAddressPage, testAddressUk)
    .withPage(IndWithoutIdUkAddressInUserAnswers, testAddressUk)

  private def withIndividualWithoutIdAnswers = baseAnswersWithValidatedMatch(registeredAddressInUk = false)
    .withPage(HaveUTRPage, false)
    .withPage(RegistrationTypePage, Individual)
    .withPage(HaveNiNumberPage, false)
    .withPage(IndWithoutNinoNamePage, Name("Individual", "WithoutId"))
    .withPage(IndWithoutIdDateOfBirthPage, LocalDate.of(1992, 3, 3))
    .withPage(WhereDoYouLivePage, false)
    .withPage(IndWithoutIdAddressNonUkPage, testIndWithoutIdAddressNonUk)

  "RegisteredAddressInUkPage" - {
    "cleanup" - {
      "when user changes from No to Yes from CYA" - {
        "must clear organisation-without-id branch answers, clears match flag and safeId but retain organisation contact details" in {
          val result = RegisteredAddressInUkPage
            .cleanup(newValue = true, updatedUserAnswers = withOrganisationWithoutIdAnswers, hasChanged = true)
            .success
            .value

          result.get(HaveUTRPage)                     mustBe empty
          result.get(OrgWithoutIdBusinessNamePage)    mustBe empty
          result.get(HaveTradingNamePage)             mustBe empty
          result.get(TradingNamePage)                 mustBe empty
          result.get(OrganisationBusinessAddressPage) mustBe empty

          result.get(FirstContactNamePage)  mustBe Some(firstContactName)
          result.get(FirstContactEmailPage) mustBe Some(firstContactEmail)
          result.hasValidMatch              mustBe false
          result.safeId                     mustBe None
        }

        "must clear sole-trader-with-NINO branch answers, clear match flag and safeId but retain Ind/ST Contact Details" in {
          val result = RegisteredAddressInUkPage
            .cleanup(newValue = true, updatedUserAnswers = withSoleTraderWithNinoAnswers, hasChanged = true)
            .success
            .value

          result.get(HaveUTRPage)                  mustBe empty
          result.get(HaveNiNumberPage)             mustBe empty
          result.get(NiNumberPage)                 mustBe empty
          result.get(WhatIsYourNameIndividualPage) mustBe empty
          result.get(RegisterDateOfBirthPage)      mustBe empty

          result.get(IndividualEmailPage)       mustBe Some(individualContactEmail)
          result.get(IndividualHavePhonePage)   mustBe Some(true)
          result.get(IndividualPhoneNumberPage) mustBe Some(individualContactPhone)
          result.hasValidMatch                  mustBe false
          result.safeId                         mustBe None
        }

        "must clear individual-with-NINO branch answers but retain Ind/ST Contact details" in {
          val result = RegisteredAddressInUkPage
            .cleanup(newValue = true, updatedUserAnswers = withIndividualWithNinoAnswers, hasChanged = true)
            .success
            .value

          result.get(HaveUTRPage)                  mustBe empty
          result.get(HaveNiNumberPage)             mustBe empty
          result.get(NiNumberPage)                 mustBe empty
          result.get(WhatIsYourNameIndividualPage) mustBe empty
          result.get(RegisterDateOfBirthPage)      mustBe empty

          result.get(IndividualEmailPage)       mustBe Some(individualContactEmail)
          result.get(IndividualHavePhonePage)   mustBe Some(true)
          result.get(IndividualPhoneNumberPage) mustBe Some(individualContactPhone)
          result.hasValidMatch                  mustBe false
          result.safeId                         mustBe None
        }

        "must clear sole-trader-without-id branch answers but retain Ind/ST Contact details" in {
          val result = RegisteredAddressInUkPage
            .cleanup(newValue = true, updatedUserAnswers = withSoleTraderWithoutIdAnswers, hasChanged = true)
            .success
            .value

          result.get(HaveUTRPage)                           mustBe empty
          result.get(HaveNiNumberPage)                      mustBe empty
          result.get(IndWithoutNinoNamePage)                mustBe empty
          result.get(IndWithoutIdDateOfBirthPage)           mustBe empty
          result.get(WhereDoYouLivePage)                    mustBe empty
          result.get(IndFindAddressAdditionalCallUa)        mustBe empty
          result.get(IndFindAddressPage)                    mustBe empty
          result.get(IndWithoutIdAddressPagePrePop)         mustBe empty
          result.get(IndWithoutIdChooseAddressPage)         mustBe empty
          result.get(IndWithoutIdSelectedChooseAddressPage) mustBe empty
          result.get(IndWithoutIdUkAddressInUserAnswers)    mustBe empty

          result.get(IndividualEmailPage)       mustBe Some(individualContactEmail)
          result.get(IndividualHavePhonePage)   mustBe Some(true)
          result.get(IndividualPhoneNumberPage) mustBe Some(individualContactPhone)
          result.hasValidMatch                  mustBe false
          result.safeId                         mustBe None
        }

        "must clear individual-without-id branch answers but retain Ind/ST Contact details" in {
          val result = RegisteredAddressInUkPage
            .cleanup(newValue = true, updatedUserAnswers = withIndividualWithoutIdAnswers, hasChanged = true)
            .success
            .value

          result.get(HaveUTRPage)                  mustBe empty
          result.get(HaveNiNumberPage)             mustBe empty
          result.get(IndWithoutNinoNamePage)       mustBe empty
          result.get(IndWithoutIdDateOfBirthPage)  mustBe empty
          result.get(WhereDoYouLivePage)           mustBe empty
          result.get(IndWithoutIdAddressNonUkPage) mustBe empty

          result.get(IndividualEmailPage)       mustBe Some(individualContactEmail)
          result.get(IndividualHavePhonePage)   mustBe Some(true)
          result.get(IndividualPhoneNumberPage) mustBe Some(individualContactPhone)
          result.hasValidMatch                  mustBe false
          result.safeId                         mustBe None
        }
      }

      "when user re-submits the same No answer from CYA (No -> No)" - {
        "must not clear any answers" in {
          val existingAnswers = withOrganisationWithoutIdAnswers

          val result = RegisteredAddressInUkPage
            .cleanup(newValue = false, updatedUserAnswers = existingAnswers, hasChanged = false)
            .success
            .value

          result mustBe existingAnswers
        }
      }

      /*
      This path is a defensive backend guard, it's not reachable via UX
      because CYA does not expose the /change-registered-address-in-uk
      for this journey.
      */
      "when user changes from Yes to No" - {
        "must retain existing answers, validated match state and safeId" in {
          val existingAnswers = baseAnswersWithValidatedMatch(registeredAddressInUk = true)
            .withPage(HaveUTRPage, false)
            .withPage(RegistrationTypePage, LLP)
            .withPage(OrgWithoutIdBusinessNamePage, organisationBusinessName)
            .withPage(HaveTradingNamePage, true)
            .withPage(TradingNamePage, organisationTradingName)
            .withPage(OrganisationBusinessAddressPage, testBusinessAddress)

          val result = RegisteredAddressInUkPage
            .cleanup(newValue = false, updatedUserAnswers = existingAnswers, hasChanged = true)
            .success
            .value

          result.get(HaveUTRPage)                  mustBe Some(false)
          result.get(OrgWithoutIdBusinessNamePage) mustBe Some(organisationBusinessName)
          result.get(HaveNiNumberPage)             mustBe empty
          result.get(IndWithoutNinoNamePage)       mustBe empty
          result.get(FirstContactEmailPage)        mustBe Some(firstContactEmail)
          result.hasValidMatch                     mustBe true
          result.safeId                            mustBe Some(SafeId(testSafeId))
        }
      }
    }
  }
}
