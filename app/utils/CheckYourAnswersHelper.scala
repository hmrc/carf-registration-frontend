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

import models.UserAnswers
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.*
import viewmodels.checkAnswers.organisation.{FirstContactEmailSummary, FirstContactNameSummary, FirstContactPhoneSummary, OrganisationRegistrationTypeSummary, RegisteredAddressInUkSummary, YourUniqueTaxpayerReferenceSummary}

class CheckYourAnswersHelper(
    val userAnswers: UserAnswers
)(implicit val messages: Messages) {

  def haveTradingName: Option[SummaryListRow] = HaveTradingNameSummary.row(userAnswers)

  def whatIsYourNameIndividual: Option[SummaryListRow] = WhatIsYourNameIndividualSummary.row(userAnswers)

  def registerDateOfBirth: Option[SummaryListRow] = RegisterDateOfBirthSummary.row(userAnswers)

  def whatIsYourNameOrganisation: Option[SummaryListRow] = WhatIsYourNameSummary.row(userAnswers)

  def niNumber: Option[SummaryListRow] = NiNumberSummary.row(userAnswers)

  def organisationRegistrationType: Option[SummaryListRow] = OrganisationRegistrationTypeSummary.row(userAnswers)

  def registeredAddressInUk: Option[SummaryListRow] = RegisteredAddressInUkSummary.row(userAnswers)

  def yourUniqueTaxpayerReference: Option[SummaryListRow] =
    YourUniqueTaxpayerReferenceSummary.row(userAnswers)

  def firstContactEmail: Option[SummaryListRow] = FirstContactEmailSummary.row(userAnswers)

  def firstContactName: Option[SummaryListRow] = FirstContactNameSummary.row(userAnswers)

  def firstContactPhone: Option[SummaryListRow] = FirstContactPhoneSummary.row(userAnswers)

}
