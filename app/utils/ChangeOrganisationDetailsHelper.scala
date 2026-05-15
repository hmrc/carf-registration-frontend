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

package utils

import com.google.inject.Inject
import models.UserAnswers
import pages.changeContactDetails.{ChangeDetailsOrgFirstHavePhonePage, ChangeDetailsOrgHaveSecondContactPage, ChangeDetailsOrgSecondHavePhonePage}
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.changeContactDetails.*

class ChangeOrganisationDetailsHelper @Inject() extends Logging {

  def getFirstContactDetailsSectionMaybe(
      userAnswers: UserAnswers
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    for {
      nameRow      <- ChangeDetailsOrgFirstNameSummary.row(userAnswers)
      emailRow     <- ChangeDetailsOrgFirstEmailSummary.row(userAnswers)
      havePhone    <- userAnswers.get(ChangeDetailsOrgFirstHavePhonePage)
      havePhoneRow <- ChangeDetailsOrgFirstHavePhoneSummary.row(userAnswers)
    } yield
      if (havePhone) {
        ChangeDetailsOrgFirstPhoneNumberSummary.row(userAnswers).map {
          Seq(nameRow, emailRow, havePhoneRow, _)
        }
      } else {
        Some(Seq(nameRow, emailRow, havePhoneRow))
      }
  }.flatten

  def getSecondContactDetailsSectionMaybe(
      userAnswers: UserAnswers
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    for {
      haveSecondContact    <- userAnswers.get(ChangeDetailsOrgHaveSecondContactPage)
      haveSecondContactRow <- ChangeDetailsOrgHaveSecondContactSummary.row(userAnswers)
    } yield
      if (haveSecondContact) {
        secondContactDetailsSectionMaybe(userAnswers).map {
          haveSecondContactRow +: _
        }
      } else {
        Some(Seq(haveSecondContactRow))
      }
  }.flatten

  private def secondContactDetailsSectionMaybe(
      userAnswers: UserAnswers
  )(implicit messages: Messages): Option[Seq[SummaryListRow]] = {
    for {
      nameRow      <- ChangeDetailsOrgSecondNameSummary.row(userAnswers)
      emailRow     <- ChangeDetailsOrgSecondEmailSummary.row(userAnswers)
      havePhone    <- userAnswers.get(ChangeDetailsOrgSecondHavePhonePage)
      havePhoneRow <- ChangeDetailsOrgSecondHavePhoneSummary.row(userAnswers)
    } yield
      if (havePhone) {
        ChangeDetailsOrgSecondPhoneNumberSummary.row(userAnswers).map {
          Seq(nameRow, emailRow, havePhoneRow, _)
        }
      } else {
        Some(Seq(nameRow, emailRow, havePhoneRow))
      }
  }.flatten
}
