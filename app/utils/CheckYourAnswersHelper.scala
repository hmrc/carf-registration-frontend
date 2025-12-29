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
import models.UserAnswers
import pages.organisation.{FirstContactPhonePage, OrganisationHaveSecondContactPage, OrganisationSecondContactHavePhonePage}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.Section
import viewmodels.checkAnswers.IsThisYourBusinessSummary
import viewmodels.checkAnswers.organisation.*

class CheckYourAnswersHelper @Inject() {

  def getBusinessDetailsSectionMaybe(userAnswers: UserAnswers)(implicit messages: Messages): Option[Section] =
    IsThisYourBusinessSummary
      .row(userAnswers)
      .map(row => Section(messages("checkYourAnswers.summaryListTitle.businessDetails"), Seq(row)))

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
            // TODO: Org second contact phone number is missing, integrate when it is merged
            FirstContactPhoneNumberSummary.row(userAnswers).map { secondPhoneNumber =>
              Seq(
                doYouHaveSecondContactRow,
                secondContactName,
                secondContactEmail,
                canWeContactSecondContact,
                secondPhoneNumber
              )
            }
          } else {
            Some(Seq(doYouHaveSecondContactRow, secondContactName, secondContactEmail, canWeContactSecondContact))
          }
      } else {
        Some(Some(Seq(doYouHaveSecondContactRow)))
      }
    }.flatten
  }.flatten.map(Section(messages("checkYourAnswers.summaryListTitle.secondContact"), _))
}
