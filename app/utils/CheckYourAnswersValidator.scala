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

import models.{IndividualRegistrationType, UserAnswers}
import pages.*
import pages.individual.{HaveNiNumberPage, IndividualRegistrationTypePage, NiNumberPage}
import play.api.libs.json.Reads

sealed trait IndividualAnswersValidator {
  self: CheckYourAnswersValidator =>

  private def checkIndividualWithIdMissingAnswers: Seq[Page] = Seq(
    checkPage(NiNumberPage),
    checkPage(WhatIsYourNameIndividualPage)
  ).flatten

  def checkIndividualMissingAnswers: Seq[Page] = userAnswers.get(HaveNiNumberPage) match {
    case Some(false) => Seq(NiNumberPage)
    case Some(true)  => checkIndividualWithIdMissingAnswers
    case _           => Seq(HaveNiNumberPage)
  }

}

class CheckYourAnswersValidator(val userAnswers: UserAnswers) extends IndividualAnswersValidator {

  private[utils] val registrationType = userAnswers.get(IndividualRegistrationTypePage)
  private[utils] val isAutoMatchedUtr = userAnswers.get(IsThisYourBusinessPage).isDefined

  private[utils] def checkPage[A](page: QuestionPage[A])(implicit rds: Reads[A]): Option[Page] =
    userAnswers.get(page) match {
      case None => Some(page)
      case _    => None
    }

  private[utils] def checkPageWithAnswer[A](page: QuestionPage[A], answer: A)(implicit rds: Reads[A]): Option[Page] =
    userAnswers.get(page) match {
      case Some(a) if a == answer => None
      case _                      => Some(page)
    }

  private[utils] def any(checkPages: Option[Page]*): Option[Page] =
    checkPages.find(_.isEmpty).getOrElse(checkPages.last)

  def validate: Seq[Page] =
    (registrationType, isAutoMatchedUtr) match {
      case (Some(IndividualRegistrationType.Individual), _) => checkIndividualMissingAnswers
      case _                                                => Seq(IndividualRegistrationTypePage)
    }

}

object CheckYourAnswersValidator {

  def apply(userAnswers: UserAnswers): CheckYourAnswersValidator =
    new CheckYourAnswersValidator(userAnswers)
}
