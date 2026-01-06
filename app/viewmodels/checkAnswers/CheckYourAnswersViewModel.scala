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

import models.UserAnswers
import pages.organisation.FirstContactPhonePage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{CheckYourAnswersHelper, UserAnswersHelper}
import viewmodels.Section

object CheckYourAnswersViewModel extends UserAnswersHelper {

  def buildPages(userAnswers: UserAnswers)(implicit
      messages: Messages
  ): Seq[Section] = {

    val isBusiness                     = isRegisteringAsBusiness(userAnswers)
    val helper: CheckYourAnswersHelper = new CheckYourAnswersHelper(userAnswers)

    val contactHeading = messages(s"checkYourAnswers.title")

    Seq(
      Section(contactHeading, buildFirstContact(helper, isBusiness))
    )
  }

  private def buildFirstContact(helper: CheckYourAnswersHelper, isBusiness: Boolean): Seq[SummaryListRow] = {

    val individualPhoneRow = helper.userAnswers.get(FirstContactPhonePage).flatMap {
      case true  => helper.firstContactEmail
      case false => None
    }
    Seq(
      helper.whatIsYourNameIndividual,
      helper.firstContactEmail,
      helper.firstContactPhone
    ).flatten ++ individualPhoneRow
  }
}
