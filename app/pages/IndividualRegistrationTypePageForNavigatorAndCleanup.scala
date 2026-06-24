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

import models.RegistrationType.{Individual, SoleTrader}
import models.{RegistrationType, UserAnswers}
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}
import pages.organisation.*
import play.api.libs.json.JsPath

import scala.util.{Success, Try}

case object IndividualRegistrationTypePageForNavigatorAndCleanup extends QuestionPage[RegistrationType] {
  override def path: JsPath = JsPath \ toString

  override def toString: String = "individualRegistrationType"

  private val nonSoleTraderPages = List(
    WhatIsTheNameOfYourBusinessPage,
    IsThisYourBusinessPage,
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

  private val nonIndNotConnectedToABusinessPages = List(
    RegisteredAddressInUkPage,
    HaveUTRPage,
    UniqueTaxpayerReferenceInUserAnswers,
    WhatIsYourNamePage,
    IsThisYourBusinessPage
  )

  override def cleanup(
      newValue: RegistrationType,
      updatedUserAnswers: UserAnswers,
      hasChanged: Boolean
  ): Try[UserAnswers] =
    if (hasChanged) {
      // CARF-545: Don't clear match flag when changing between SoleTrader and Individual on /individual-registration-type
      if (newValue == SoleTrader) {
        updatedUserAnswers.remove(nonSoleTraderPages)
      } else if (newValue == Individual) {
        updatedUserAnswers.remove(nonIndNotConnectedToABusinessPages)
      } else {
        throw new IllegalArgumentException(
          "Registration type from /individual-registration-type can only be SoleTrader or Individual"
        )
      }
    } else {
      Success(updatedUserAnswers)
    }
}
