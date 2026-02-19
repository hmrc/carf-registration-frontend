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

import com.google.inject.Inject
import models.{RegistrationType, UserAnswers}
import pages.individual.{HaveNiNumberPage, IndividualHavePhonePage}
import pages.orgWithoutId.HaveTradingNamePage
import pages.organisation.*
import pages.{RegisteredAddressInUkPage, WhereDoYouLivePage}
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.Section
import viewmodels.checkAnswers.individual.*
import viewmodels.checkAnswers.individualWithoutId.{IndWithoutIdAddressNonUkSummary, IndWithoutIdAddressUkSummary, IndWithoutIdDateOfBirthSummary, IndWithoutNinoNameSummary}
import viewmodels.checkAnswers.orgWithoutId.{HaveTradingNameSummary, OrgWithoutIdBusinessNameSummary, OrganisationBusinessAddressSummary, TradingNameSummary}
import viewmodels.checkAnswers.organisation.*
import viewmodels.checkAnswers.{IsThisYourBusinessSummary, RegisteredAddressInUkSummary}

class CheckYourAnswersHelper @Inject() extends Logging {

  def getBusinessDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    IsThisYourBusinessSummary
      .row(userAnswers)
      .map(row => Section(messages("checkYourAnswers.summaryListTitle.businessDetails"), Seq(row)))

  def getOrgWithoutIdDetailsMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] = {
    for {
      registeringAs            <- OrganisationRegistrationTypeSummary.row(userAnswers)
      registeredAddressInUkRow <- RegisteredAddressInUkSummary.row(userAnswers)
      registeredAddressInUk    <- userAnswers.get(RegisteredAddressInUkPage)
      haveUtrRow               <- HaveUTRSummary.row(userAnswers)
      haveUtr                  <- userAnswers.get(HaveUTRPage)
      businessName             <- OrgWithoutIdBusinessNameSummary.row(userAnswers)
      address                  <- OrganisationBusinessAddressSummary.row(userAnswers)
      haveTradingName: Boolean <- userAnswers.get(HaveTradingNamePage)
      haveTradingNameRow       <- HaveTradingNameSummary.row(userAnswers)
    } yield
      val hasCorrectAnswersForGettingHere: Boolean = !registeredAddressInUk && !haveUtr
      if (hasCorrectAnswersForGettingHere) {
        if (haveTradingName) {
          TradingNameSummary
            .row(userAnswers)
            .map(Seq(registeringAs, registeredAddressInUkRow, haveUtrRow, businessName, haveTradingNameRow, _, address))
        } else {
          Some(Seq(registeringAs, registeredAddressInUkRow, haveUtrRow, businessName, haveTradingNameRow, address))
        }
      } else {
        None
      }
  }.flatten.map(Section(messages("checkYourAnswers.summaryListTitle.businessDetails"), _))

  def indWithNinoYourDetailsMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] = {
    for {
      registeringAs     <- IndividualRegistrationTypeSummary.row(userAnswers)
      haveNinoRow       <- HaveNiNumberSummary.row(userAnswers)
      haveNino: Boolean <- userAnswers.get(HaveNiNumberPage)
      whatNino          <- NiNumberSummary.row(userAnswers)
      name              <- WhatIsYourNameIndividualSummary.row(userAnswers)
      dob               <- RegisterDateOfBirthSummary.row(userAnswers)
      registrationType  <- userAnswers.get(RegistrationTypePage)
    } yield
      if (haveNino) {
        registrationType match {
          case RegistrationType.SoleTrader =>
            {
              for {
                registeredAddressInUkRow <- RegisteredAddressInUkSummary.row(userAnswers)
                registeredAddressInUk    <- userAnswers.get(RegisteredAddressInUkPage)
                haveUtrRow               <- HaveUTRSummary.row(userAnswers)
                haveUtr                  <- userAnswers.get(HaveUTRPage)
              } yield {
                val hasCorrectAnswersForGettingHere: Boolean = !registeredAddressInUk && !haveUtr && haveNino
                if (hasCorrectAnswersForGettingHere) {
                  Some(Seq(registeringAs, registeredAddressInUkRow, haveUtrRow, haveNinoRow, whatNino, name, dob))
                } else {
                  logger.warn("Individual with NINO answers were not as expected")
                  None
                }
              }
            }.flatten
          case RegistrationType.Individual => Some(Seq(registeringAs, haveNinoRow, whatNino, name, dob))
          case _                           =>
            None
        }
      } else {
        logger.warn(s"Individual with NINO requires user to have a nino. When questioned, user answered: $haveNino")
        None
      }
  }.flatten.map(Section(messages("checkYourAnswers.summaryListTitle.individualDetails"), _))

  def indWithoutIdYourDetailsMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] = {
    for {
      registeringAs     <- IndividualRegistrationTypeSummary.row(userAnswers)
      registrationType  <- userAnswers.get(RegistrationTypePage)
      haveNinoRow       <- HaveNiNumberSummary.row(userAnswers)
      haveNino: Boolean <- userAnswers.get(HaveNiNumberPage)
      name              <- IndWithoutNinoNameSummary.row(userAnswers)
      dob               <- IndWithoutIdDateOfBirthSummary.row(userAnswers)
      liveInUkOrCd      <- userAnswers.get(WhereDoYouLivePage)
      address           <- getAddressRowMaybe(userAnswers, liveInUkOrCd)
    } yield
      if (!haveNino) {
        registrationType match {
          case RegistrationType.SoleTrader =>
            {
              for {
                registeredAddressInUkRow <- RegisteredAddressInUkSummary.row(userAnswers)
                registeredAddressInUk    <- userAnswers.get(RegisteredAddressInUkPage)
                haveUtrRow               <- HaveUTRSummary.row(userAnswers)
                haveUtr                  <- userAnswers.get(HaveUTRPage)
              } yield {
                val hasCorrectAnswersForGettingHere: Boolean = !registeredAddressInUk && !haveUtr && !haveNino
                if (hasCorrectAnswersForGettingHere) {
                  Some(Seq(registeringAs, registeredAddressInUkRow, haveUtrRow, haveNinoRow, name, dob, address))
                } else {
                  logger.warn("Individual without NINO answers were not as expected")
                  None
                }
              }
            }.flatten
          case RegistrationType.Individual => Some(Seq(registeringAs, haveNinoRow, name, dob, address))
          case _                           =>
            None
        }
      } else {
        logger.warn(
          s"Individual without NINO requires user to NOT have a nino. When questioned, user answered: $haveNino"
        )
        None
      }
  }.flatten.map(Section(messages("checkYourAnswers.summaryListTitle.individualDetails"), _))

  def getFirstContactDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] = {
    for {
      firstContactName               <- FirstContactNameSummary.row(userAnswers)
      firstContactEmail              <- FirstContactEmailSummary.row(userAnswers)
      canWeContactFirstContact       <- FirstContactPhoneSummary.row(userAnswers)
      canWeContactFirstContactAnswer <- userAnswers.get(FirstContactPhonePage)
    } yield
      if (canWeContactFirstContactAnswer) {
        FirstContactPhoneNumberSummary.row(userAnswers).map {
          Seq(firstContactName, firstContactEmail, canWeContactFirstContact, _)
        }
      } else {
        Some(Seq(firstContactName, firstContactEmail, canWeContactFirstContact))
      }
  }.flatten.map(Section(messages("checkYourAnswers.summaryListTitle.firstContact"), _))

  def getSecondContactDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] = {
    for {
      doYouHaveSecondContact    <- userAnswers.get(OrganisationHaveSecondContactPage)
      doYouHaveSecondContactRow <- OrganisationHaveSecondContactSummary.row(userAnswers)
    } yield {
      if (doYouHaveSecondContact) {
        for {
          secondContactName               <- OrganisationSecondContactNameSummary.row(userAnswers)
          secondContactEmail              <- OrganisationSecondContactEmailSummary.row(userAnswers)
          canWeContactSecondContact       <- OrganisationSecondContactHavePhoneSummary.row(userAnswers)
          canWeContactSecondContactAnswer <- userAnswers.get(OrganisationSecondContactHavePhonePage)
        } yield
          if (canWeContactSecondContactAnswer) {
            OrganisationSecondContactPhoneNumberSummary.row(userAnswers).map {
              Seq(doYouHaveSecondContactRow, secondContactName, secondContactEmail, canWeContactSecondContact, _)
            }
          } else {
            Some(Seq(doYouHaveSecondContactRow, secondContactName, secondContactEmail, canWeContactSecondContact))
          }
      } else {
        Some(Some(Seq(doYouHaveSecondContactRow)))
      }
    }.flatten
  }.flatten.map(Section(messages("checkYourAnswers.summaryListTitle.secondContact"), _))

  def indContactDetailsMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] = {
    for {
      email        <- IndividualEmailSummary.row(userAnswers)
      havePhoneRow <- IndividualHavePhoneSummary.row(userAnswers)
      havePhone    <- userAnswers.get(IndividualHavePhonePage)
    } yield
      if (havePhone) {
        IndividualPhoneNumberSummary.row(userAnswers).map(Seq(email, havePhoneRow, _))
      } else {
        Some(Seq(email, havePhoneRow))
      }
  }.flatten.map(Section(messages("checkYourAnswers.summaryListTitle.individualContactDetails"), _))

  private def getAddressRowMaybe(userAnswers: UserAnswers, liveInUkOrCd: Boolean)(implicit
      messages: Messages
  ): Option[SummaryListRow] =
    if (liveInUkOrCd) {
      IndWithoutIdAddressUkSummary.row(userAnswers)
    } else {
      IndWithoutIdAddressNonUkSummary.row(userAnswers)
    }
}
