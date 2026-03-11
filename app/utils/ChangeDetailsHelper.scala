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
import pages.changeContactDetails.ChangeDetailsIndividualHavePhonePage
import pages.individual.{HaveNiNumberPage, IndividualHavePhonePage}
import pages.orgWithoutId.HaveTradingNamePage
import pages.organisation.*
import pages.{RegisteredAddressInUkPage, WhereDoYouLivePage}
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.Section
import viewmodels.checkAnswers.changeContactDetails.{ChangeDetailsIndividualEmailSummary, ChangeDetailsIndividualHavePhoneSummary, ChangeDetailsIndividualPhoneNumberSummary}
import viewmodels.checkAnswers.individual.*
import viewmodels.checkAnswers.individualWithoutId.{IndWithoutIdAddressNonUkSummary, IndWithoutIdAddressUkSummary, IndWithoutIdDateOfBirthSummary, IndWithoutNinoNameSummary}
import viewmodels.checkAnswers.orgWithoutId.{HaveTradingNameSummary, OrgWithoutIdBusinessNameSummary, OrganisationBusinessAddressSummary, TradingNameSummary}
import viewmodels.checkAnswers.organisation.*
import viewmodels.checkAnswers.{IsThisYourBusinessSummary, RegisteredAddressInUkSummary}

class ChangeDetailsHelper @Inject() extends Logging {

  def getFirstContactDetailsSectionMaybe(
      userAnswers: UserAnswers
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    for {
      emailRow     <- ChangeDetailsIndividualEmailSummary.row(userAnswers)
      havePhone    <- userAnswers.get(ChangeDetailsIndividualHavePhonePage)
      havePhoneRow <- ChangeDetailsIndividualHavePhoneSummary.row(userAnswers)
    } yield
      if (havePhone) {
        ChangeDetailsIndividualPhoneNumberSummary.row(userAnswers).map {
          Seq(emailRow, havePhoneRow, _)
        }
      } else {
        Some(Seq(emailRow, havePhoneRow))
      }
  }.flatten

}
