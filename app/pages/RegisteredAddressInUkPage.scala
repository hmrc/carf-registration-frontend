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

import models.UserAnswers
import pages.individual.{HaveNiNumberPage, NiNumberPage, RegisterDateOfBirthPage, WhatIsYourNameIndividualPage}
import pages.individualWithoutId.*
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}
import pages.organisation.HaveUTRPage
import play.api.libs.json.JsPath

import scala.util.{Success, Try}

case object RegisteredAddressInUkPage extends QuestionPage[Boolean] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "registeredAddressInUk"

  private val orgWithoutUtrPages = List(
    OrgWithoutIdBusinessNamePage,
    HaveTradingNamePage,
    TradingNamePage,
    OrganisationBusinessAddressPage
  )

  private val indWithNinoPages = List(
    HaveNiNumberPage,
    NiNumberPage,
    WhatIsYourNameIndividualPage,
    RegisterDateOfBirthPage
  )

  private val indWithoutIdPages = List(
    HaveNiNumberPage,
    IndWithoutNinoNamePage,
    IndWithoutIdDateOfBirthPage,
    WhereDoYouLivePage,
    IndFindAddressAdditionalCallUa,
    IndFindAddressPage,
    IndWithoutIdAddressNonUkPage,
    IndWithoutIdAddressPagePrePop,
    IndWithoutIdChooseAddressPage,
    IndWithoutIdSelectedChooseAddressPage,
    IndWithoutIdUkAddressInUserAnswers
  )

  private val noToYesPagesToRemove = HaveUTRPage :: (orgWithoutUtrPages ++ indWithNinoPages ++ indWithoutIdPages)

  override def cleanup(
      newValue: Boolean,
      updatedUserAnswers: UserAnswers,
      hasChanged: Boolean
  ): Try[UserAnswers] =
    if (newValue) {
      updatedUserAnswers.clearMatchFlagAndSafeId
        .remove(noToYesPagesToRemove)
    } else {
      Success(updatedUserAnswers)
    }
}
