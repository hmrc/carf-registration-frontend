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
import controllers.routes.{CheckYourAnswersController, IsThisYourBusinessController}
import models.RegistrationType.SoleTrader
import models.{ChangeMode, NormalMode, RegistrationType, UserAnswers}
import pages.organisation.{HaveUTRPage, NavigatorOnlyIndividualRegistrationTypePage, NavigatorOnlyOrganisationRegistrationTypePage, RegistrationTypePage, WhatIsTheNameOfYourBusinessPage, WhatIsYourNamePage, YourUtrPageForNavigatorOnly}
import pages.{IsThisYourBusinessPage, Page, QuestionPage, RegisteredAddressInUkPage}
import play.api.libs.json.Reads
import play.api.mvc.Call
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import utils.UserAnswersHelper

trait ChangeRoutesNavigator extends UserAnswersHelper {

  val checkRouteMap: Page => UserAnswers => Call = {
    case NavigatorOnlyIndividualRegistrationTypePage =>
      userAnswers => navigateFromIndividualRegistrationTypePage(userAnswers)

    case NavigatorOnlyOrganisationRegistrationTypePage =>
      _ => controllers.routes.RegisteredAddressInUkController.onPageLoad(ChangeMode)

    case RegisteredAddressInUkPage =>
      userAnswers => navigateFromRegisteredAddressInUk(userAnswers)

    case HaveUTRPage =>
      userAnswers => navigateFromHaveUTR(userAnswers)

    case YourUtrPageForNavigatorOnly =>
      userAnswers => navigateFromYourUniqueTaxpayerReference(userAnswers)

    case WhatIsTheNameOfYourBusinessPage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(ChangeMode)

    case WhatIsYourNamePage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(ChangeMode)

    case IsThisYourBusinessPage =>
      userAnswers =>
        checkNextPageForValueThenRoute(
          userAnswers = userAnswers,
          page = IsThisYourBusinessPage,
          callWhenNotAnswered = getContactDetailsRoute(userAnswers)
        )

    case _ => _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def checkNextPageForValueThenRoute[A](
      userAnswers: UserAnswers,
      page: QuestionPage[A],
      callWhenNotAnswered: Call,
      callWhenAlreadyAnswered: Call = CheckYourAnswersController.onPageLoad()
  )(implicit rds: Reads[A]): Call = {
    val answerExists = userAnswers.get(page).fold(false)(_ => true)
    if (answerExists) {
      callWhenAlreadyAnswered
    } else {
      callWhenNotAnswered
    }
  }

  private def navigateFromIndividualRegistrationTypePage(userAnswers: UserAnswers): Call =
    userAnswers.get(RegistrationTypePage) match {
      case Some(SoleTrader)                  =>
        controllers.routes.RegisteredAddressInUkController.onPageLoad(ChangeMode)
      case Some(RegistrationType.Individual) =>
        controllers.individual.routes.HaveNiNumberController.onPageLoad(NormalMode)
      case _                                 =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  // TODO: investigate this if person did RAIUK change link or Reg Type change link rather than your business change link
  // TODO: Clear matching flag if any other info changes
  private def navigateFromRegisteredAddressInUk(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredAddressInUkPage) match {
      case Some(true)  =>
        controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(ChangeMode)
      case Some(false) =>
        controllers.organisation.routes.HaveUTRController.onPageLoad(ChangeMode)
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromHaveUTR(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveUTRPage) match {
      case Some(true)  =>
        controllers.organisation.routes.YourUniqueTaxpayerReferenceController.onPageLoad(ChangeMode)
      case Some(false) =>
        if (isSoleTrader(userAnswers)) {
          controllers.individual.routes.HaveNiNumberController.onPageLoad(NormalMode)
        } else if (userAnswers.get(RegistrationTypePage).isDefined) {
          controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(ChangeMode)
        } else {
          routes.JourneyRecoveryController.onPageLoad()
        }
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromYourUniqueTaxpayerReference(userAnswers: UserAnswers): Call =
    if (isSoleTrader(userAnswers)) {
      controllers.organisation.routes.WhatIsYourNameController.onPageLoad(ChangeMode)
    } else {
      controllers.organisation.routes.WhatIsTheNameOfYourBusinessController.onPageLoad(ChangeMode)
    }

  private def getContactDetailsRoute(userAnswers: UserAnswers): Call =
    if (userAnswers.affinityGroup == Individual) {
      controllers.individual.routes.IndividualEmailController.onPageLoad(NormalMode)
    } else {
      controllers.organisation.routes.OrgYourContactDetailsController.onPageLoad()
    }

}
