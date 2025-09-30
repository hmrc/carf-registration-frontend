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

package navigation

import controllers.routes
import models.{IndividualRegistrationType, UserAnswers}
import pages.{IndividualRegistrationTypePage, OrganisationRegistrationTypePage, Page}
import play.api.mvc.Call

trait NormalRoutesNavigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case IndividualRegistrationTypePage   =>
      userAnswers => navigateFromIndividualRegistrationTypePage(userAnswers)
    case OrganisationRegistrationTypePage =>
      _ => routes.PlaceholderController.onPageLoad("Must redirect to /registered-address-in-uk (CARF-121)")
    case _                                => _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromIndividualRegistrationTypePage(userAnswers: UserAnswers): Call =
    userAnswers.get(IndividualRegistrationTypePage) match {
      case Some(IndividualRegistrationType.SoleTrader) =>
        routes.PlaceholderController.onPageLoad("Must redirect to /registered-address-in-uk (CARF-121)")
      case Some(IndividualRegistrationType.Individual) =>
        routes.PlaceholderController.onPageLoad("Must redirect to /have-ni-number (CARF-163)")
      case _                                           =>
        routes.JourneyRecoveryController.onPageLoad()
    }
}
