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

import cats.implicits.*
import cats.syntax.all.*
import com.google.inject.Inject
import models.UserAnswers
import models.error.ApiError.ApplicationError
import models.error.{CarfError, DataError}
import models.responses.hasIndividualChangedData
import pages.changeContactDetails.{ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage}
import play.api.Logging
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.changeContactDetails.*

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

  def getHasChanged(
      maybeEmail: Option[String],
      maybeHavePhone: Option[Boolean],
      userAnswers: UserAnswers
  ): Either[CarfError, Boolean] =
    for {
      displaySubResponse <- userAnswers.displaySubscriptionResponse.toRight[CarfError] {
                              logger.debug(
                                s"Subscription Response is missing and cannot continue to see what has changed"
                              )
                              ApplicationError
                            }
      maybeDetails       <-
        (maybeEmail, maybeHavePhone).mapN((email, havePhone) => (email, havePhone)).toRight {
          logger.debug(s"data is missing and cannot be used for comparison")
          DataError
        }
      (email, havePhone)  = maybeDetails
      phone              <- if (havePhone) {
                              userAnswers.get(ChangeDetailsIndividualPhoneNumberPage).map(Some(_)).toRight(DataError)
                            } else {
                              Right(None)
                            }
    } yield displaySubResponse.hasIndividualChangedData(email, phone)
}
