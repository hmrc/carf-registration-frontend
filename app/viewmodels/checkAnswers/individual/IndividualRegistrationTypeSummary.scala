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

package viewmodels.checkAnswers.individual

import controllers.routes
import models.{CheckMode, RegistrationType, UserAnswers}
import pages.organisation.RegistrationTypePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object IndividualRegistrationTypeSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RegistrationTypePage).flatMap {
      case answer @ (RegistrationType.Individual | RegistrationType.SoleTrader) =>
        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"checkYourAnswers.registrationType.${answer.messagesKey}"))
          )
        )

        Some(
          SummaryListRowViewModel(
            key = "individualRegistrationType.checkYourAnswersLabel",
            value = value,
            actions = Seq(
              ActionItemViewModel(
                "site.change",
                controllers.individual.routes.IndividualRegistrationTypeController.onPageLoad(CheckMode).url
              )
                .withVisuallyHiddenText(messages("individualRegistrationType.change.hidden"))
            )
          )
        )
      case _                                                                    => None
    }
}
