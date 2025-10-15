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
import play.api.mvc.Call

trait NormalRoutesNavigator {

  val normalRoutes: Page => UserAnswers => Call = {
    case IndividualRegistrationTypePage =>
      userAnswers => navigateFromIndividualRegistrationTypePage(userAnswers)

    case OrganisationRegistrationTypePage =>
      _ => routes.RegisteredAddressInUkController.onPageLoad(NormalMode)

    case YourUniqueTaxpayerReferencePage =>
      userAnswers => navigateFromYourUniqueTaxpayerReference(userAnswers)

    case RegisteredAddressInUkPage =>
      userAnswers => navigateFromRegisteredAddressInUk(userAnswers)

    case WhatIsTheNameOfYourBusinessPage =>
      _ => routes.IsThisYourBusinessController.onPageLoad(NormalMode)

    case IsThisYourBusinessPage =>
      userAnswers => navigateFromIsThisYourBusiness(userAnswers)

    case HaveNiNumberPage =>
      userAnswers => navigateFromHaveNiNumber(userAnswers)

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
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/have-utr (Do you have a UTR page - CARF-123)"
        )
      case None        =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromYourUniqueTaxpayerReference(userAnswers: UserAnswers): Call = {

    val individualRegistrationType: Option[IndividualRegistrationType]     = userAnswers.get(IndividualRegistrationTypePage)
    val organisationRegistrationType: Option[OrganisationRegistrationType] =
      userAnswers.get(OrganisationRegistrationTypePage)

    (individualRegistrationType, organisationRegistrationType) match {
      case (Some(IndividualRegistrationType.SoleTrader), _) | (_, Some(OrganisationRegistrationType.SoleTrader)) =>
        routes.PlaceholderController.onPageLoad("Must redirect to /your-name (CARF-125)")
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
          routes.PlaceholderController.onPageLoad("Must redirect to /register/your-contact-details (CARF-177)")
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
        routes.PlaceholderController.onPageLoad("Must redirect to /ni-number (CARF-164)")
      case Some(false) =>
        routes.PlaceholderController.onPageLoad("Must redirect to /without-id/name (CARF-169)")
      case None        => routes.JourneyRecoveryController.onPageLoad()
    }
}
