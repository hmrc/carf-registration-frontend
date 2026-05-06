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

package viewmodels.checkAnswers

import models.responses.renderHTML
import models.{ChangeMode, UserAnswers}
import pages.IsThisYourBusinessPage
import play.api.i18n.Messages
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object IsThisYourBusinessSummary {

  def row(answers: UserAnswers, affinityGroup: AffinityGroup)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(IsThisYourBusinessPage).map { answer =>

      val value = HtmlContent(
        s"<p class='govuk-body govuk-!-margin-bottom-2'>${answer.businessDetails.name}</p><p class='govuk-body'>${answer.businessDetails.address.renderHTML}</p>"
      )

      val changeLinkRedirectLocation: String =
        if (answers.isCtAutoMatched) {
          controllers.organisation.routes.UnableToChangeBusinessController.onPageLoad().url
        } else {
          affinityGroup match {
            case AffinityGroup.Individual   =>
              controllers.individual.routes.IndividualRegistrationTypeController.onPageLoad(ChangeMode).url
            case AffinityGroup.Organisation =>
              controllers.organisation.routes.OrganisationRegistrationTypeController.onPageLoad(ChangeMode).url
            case _                          => throw new RuntimeException("Error! Agents not allowed in the service")
          }
        }

      SummaryListRowViewModel(
        key = "isThisYourBusiness.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel(
            content = HtmlContent(s"""<span aria-hidden='true'>${messages("site.change")}</span>"""),
            href = changeLinkRedirectLocation
          ).withVisuallyHiddenText(messages("isThisYourBusiness.change.hidden"))
        )
      )
    }
}
