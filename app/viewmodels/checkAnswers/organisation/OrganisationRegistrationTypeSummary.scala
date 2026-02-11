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

package viewmodels.checkAnswers.organisation

import controllers.organisation.routes
import models.{CheckMode, UserAnswers}
import pages.organisation.RegistrationTypePage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object OrganisationRegistrationTypeSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(RegistrationTypePage).map { answer =>

      val value = ValueViewModel(
        HtmlContent(
          HtmlFormat.escape(messages(s"checkYourAnswers.registrationType.${answer.messagesKey}"))
        )
      )

      SummaryListRowViewModel(
        key = "organisationRegistrationType.checkYourAnswersLabel",
        value = value,
        actions = Seq(
          ActionItemViewModel(
            content = HtmlContent(s"""<span aria-hidden='true'>${messages("site.change")}</span>"""),
            href = routes.OrganisationRegistrationTypeController.onPageLoad(CheckMode).url
          )
            .withVisuallyHiddenText(messages("organisationRegistrationType.change.hidden"))
        )
      )
    }
}
