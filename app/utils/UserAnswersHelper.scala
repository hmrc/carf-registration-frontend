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

import models.JourneyType.{IndWithUtr, OrgWithUtr}
import models.{IndividualRegistrationType, JourneyType, OrganisationRegistrationType, UserAnswers}
import pages.individual.IndividualRegistrationTypePage
import pages.organisation.OrganisationRegistrationTypePage

trait UserAnswersHelper {

  def isSoleTrader(userAnswers: UserAnswers): Boolean = {
    val individualRegistrationType: Option[IndividualRegistrationType]     = userAnswers.get(IndividualRegistrationTypePage)
    val organisationRegistrationType: Option[OrganisationRegistrationType] =
      userAnswers.get(OrganisationRegistrationTypePage)

    (individualRegistrationType, organisationRegistrationType) match {
      case (Some(IndividualRegistrationType.SoleTrader), _) | (_, Some(OrganisationRegistrationType.SoleTrader)) => true
      case _                                                                                                     => false
    }
  }

  def isRegisteringAsBusiness(userAnswers: UserAnswers): Boolean = {
    val individualRegistrationType: Option[IndividualRegistrationType]     = userAnswers.get(IndividualRegistrationTypePage)
    val organisationRegistrationType: Option[OrganisationRegistrationType] =
      userAnswers.get(OrganisationRegistrationTypePage)

    (individualRegistrationType, organisationRegistrationType) match {
      case (Some(IndividualRegistrationType.Individual), _) | (Some(IndividualRegistrationType.SoleTrader), _) => false
      case _                                                                                                   => true
    }
  }

  def getJourneyTypeUtrOnly(userAnswers: UserAnswers): JourneyType =
    if (isSoleTrader(userAnswers)) {
      IndWithUtr
    } else {
      OrgWithUtr
    }
}
