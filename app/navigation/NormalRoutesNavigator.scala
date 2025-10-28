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
import models.{IndividualRegistrationType, NormalMode, OrganisationRegistrationType, UserAnswers}
import models.OrganisationRegistrationType.*
import pages.*
import pages.orgWithoutId.OrgWithoutIdBusinessNamePage
import play.api.mvc.Call

trait NormalRoutesNavigator {

  val normalRoutes: Page => UserAnswers => Call = {

    case IndividualRegistrationTypePage =>
      userAnswers => navigateFromIndividualRegistrationTypePage(userAnswers)

    case OrganisationRegistrationTypePage =>
      _ => routes.RegisteredAddressInUkController.onPageLoad(NormalMode)

    case RegisteredAddressInUkPage =>
      userAnswers => navigateFromRegisteredAddressInUk(userAnswers)

    case HaveUTRPage =>
      userAnswers => navigateFromHaveUTR(userAnswers)

    case YourUniqueTaxpayerReferencePage =>
      userAnswers => navigateFromYourUniqueTaxpayerReference(userAnswers)

    case WhatIsTheNameOfYourBusinessPage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(NormalMode)

    case WhatIsYourNamePage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(NormalMode)

    case IsThisYourBusinessPage =>
      userAnswers => navigateFromIsThisYourBusiness(userAnswers)

    case HaveNiNumberPage =>
      userAnswers => navigateFromHaveNiNumber(userAnswers)

    case NiNumberPage =>
      _ => navigateFromNiNumber()

    case OrgWithoutIdBusinessNamePage =>
      userAnswers => navigateFromOrgWithoutIdBusinessName(userAnswers)

    case _ =>
      _ => routes.JourneyRecoveryController.onPageLoad()
  }

  private def navigateFromIndividualRegistrationTypePage(userAnswers: UserAnswers): Call =
    userAnswers.get(IndividualRegistrationTypePage) match {
      case Some(IndividualRegistrationType.SoleTrader) =>
        routes.RegisteredAddressInUkController.onPageLoad(NormalMode)
      case Some(IndividualRegistrationType.Individual) =>
        routes.HaveNiNumberController.onPageLoad(NormalMode)
      case _                                           =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromRegisteredAddressInUk(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredAddressInUkPage) match {
      case Some(true)  =>
        routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      case Some(false) =>
        routes.HaveUTRController.onPageLoad(NormalMode)
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromHaveUTR(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveUTRPage) match {
      case Some(true)  =>
        routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      case Some(false) =>
        if (isSoleTrader(userAnswers)) {
          routes.HaveNiNumberController.onPageLoad(NormalMode)
        } else if (userAnswers.get(OrganisationRegistrationTypePage).isDefined) {
          controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(NormalMode)
        } else {
          routes.JourneyRecoveryController.onPageLoad()
        }
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromYourUniqueTaxpayerReference(userAnswers: UserAnswers): Call = {

    val individualRegistrationType: Option[IndividualRegistrationType]     = userAnswers.get(IndividualRegistrationTypePage)
    val organisationRegistrationType: Option[OrganisationRegistrationType] =
      userAnswers.get(OrganisationRegistrationTypePage)

    (individualRegistrationType, organisationRegistrationType) match {
      case (Some(IndividualRegistrationType.SoleTrader), _) | (_, Some(OrganisationRegistrationType.SoleTrader)) =>
        routes.WhatIsYourNameController.onPageLoad(NormalMode)
      case _                                                                                                     =>
        routes.WhatIsTheNameOfYourBusinessController.onPageLoad(NormalMode)
    }
  }

  private def navigateFromIsThisYourBusiness(userAnswers: UserAnswers): Call =
    userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer) match {
      case Some(true) =>
        if (isSoleTrader(userAnswers)) {
          routes.PlaceholderController.onPageLoad("Must redirect to /register/individual-email (CARF-183)")
        } else {
          routes.OrgYourContactDetailsController.onPageLoad()
        }

      case Some(false) =>
        if (isCTAutomatched(userAnswers)) {
          routes.PlaceholderController.onPageLoad("Must redirect to /problem/different-business (CARF-127)")
        } else {
          if (isSoleTrader(userAnswers)) {
            routes.PlaceholderController.onPageLoad("Must redirect to /problem/sole-trader-not-identified (CARF-129)")
          } else {
            routes.PlaceholderController.onPageLoad("Must redirect to /problem/business-not-identified (CARF-147)")
          }
        }

      case None =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromNiNumber(): Call =
    routes.PlaceholderController.onPageLoad("Must redirect to /register/name (CARF-165)")

  private def isSoleTrader(userAnswers: UserAnswers): Boolean = {

    val individualRegistrationType: Option[IndividualRegistrationType] = userAnswers.get(IndividualRegistrationTypePage)

    val organisationRegistrationType: Option[OrganisationRegistrationType] =
      userAnswers.get(OrganisationRegistrationTypePage)

    (individualRegistrationType, organisationRegistrationType) match {
      case (Some(IndividualRegistrationType.SoleTrader), _) | (_, Some(OrganisationRegistrationType.SoleTrader)) =>
        true
      case _                                                                                                     =>
        false
    }
  }

  private def isCTAutomatched(userAnswers: UserAnswers): Boolean =
    userAnswers.get(IndexPage).isDefined

  private def navigateFromHaveNiNumber(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveNiNumberPage) match {
      case Some(true)  =>
        routes.NiNumberController.onPageLoad(NormalMode)
      case Some(false) =>
        routes.PlaceholderController.onPageLoad("Must redirect to /individual-without-id/name (CARF-169)")
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromOrgWithoutIdBusinessName(userAnswers: UserAnswers): Call =
    userAnswers.get(OrgWithoutIdBusinessNamePage) match {
      case Some(orgWithoutIdBusinessName) =>
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/business-without-id/have-trading-name (CARF-160)"
        )
      case None                           => routes.JourneyRecoveryController.onPageLoad()
    }
}
