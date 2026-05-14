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

package pages

import models.RegistrationType.{Individual, SoleTrader}
import models.{IndWithoutIdAddressNonUk, RegistrationType, UserAnswers}
import pages.individual.*
import pages.individualWithoutId.*
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}
import pages.organisation.*
import pages.{Page, QuestionPage, RegisteredAddressInUkPage}
import play.api.libs.json.JsPath

import scala.util.Try

case object RegistrationTypePage extends QuestionPage[RegistrationType] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "registrationType"

  private val nonSoleTraderPages = List(
    WhatIsTheNameOfYourBusinessPage,
    FirstContactNamePage,
    FirstContactEmailPage,
    FirstContactPhonePage,
    FirstContactPhoneNumberPage,
    OrganisationHaveSecondContactPage,
    OrganisationSecondContactNamePage,
    OrganisationSecondContactEmailPage,
    OrganisationSecondContactHavePhonePage,
    OrganisationSecondContactPhoneNumberPage,
    HaveTradingNamePage,
    TradingNamePage,
    OrgWithoutIdBusinessNamePage,
    OrganisationBusinessAddressPage
  )

  private val soleTraderPages = List(
    // Ind with utr page
    WhatIsYourNamePage,
    // General ind page
    HaveNiNumberPage,
    // Ind with NINO pages
    NiNumberPage,
    WhatIsYourNameIndividualPage,
    RegisterDateOfBirthPage,
    // Ind without id pages
    IndFindAddressAdditionalCallUa,
    IndFindAddressPage,
    IndWithoutNinoNamePage,
    IndWithoutIdAddressNonUkPage,
    IndWithoutIdAddressPagePrePop,
    IndWithoutIdChooseAddressPage,
    IndWithoutIdDateOfBirthPage,
    IndWithoutIdSelectedChooseAddressPage,
    IndWithoutIdUkAddressInUserAnswers,
    // Ind contact details pages
    IndividualEmailPage,
    IndividualHavePhonePage,
    IndividualPhoneNumberPage
  )

  private val nonIndNotConnectedToABusinessPages = List(
    RegisteredAddressInUkPage,
    HaveUTRPage
  )

  override def cleanup(
      value: RegistrationType,
      userAnswers: UserAnswers,
      hasChanged: Boolean
  ): Try[UserAnswers] = {
    val currentValue = userAnswers.get(RegistrationTypePage)
    if (hasChanged) {
      if (currentValue.contains(SoleTrader)) {
        userAnswers.copy(hasValidMatch = false).remove(nonSoleTraderPages)
      } else if (currentValue.contains(Individual)) {
        userAnswers.copy(hasValidMatch = false).remove(nonIndNotConnectedToABusinessPages)
      } else {
        userAnswers.copy(hasValidMatch = false).remove(soleTraderPages)
      }
    } else {
      super.cleanup(value, userAnswers, hasChanged)
    }
  }
}

case object NavigatorOnlyIndividualRegistrationTypePage extends Page

case object NavigatorOnlyOrganisationRegistrationTypePage extends Page
