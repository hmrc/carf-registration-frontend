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
import pages.{AutoMatchedUTRPage, HaveNiNumberPage, IndividualRegistrationTypePage, IsThisYourBusinessPage, OrganisationRegistrationTypePage, Page, RegisteredAddressInUkPage, YourUniqueTaxpayerReferencePage}
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
      case _ =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromRegisteredAddressInUk(userAnswers: UserAnswers): Call =
    userAnswers.get(RegisteredAddressInUkPage) match {
      case Some(true) =>
        routes.YourUniqueTaxpayerReferenceController.onPageLoad(NormalMode)
      case Some(false) =>
        routes.PlaceholderController.onPageLoad(
          "Must redirect to /register/have-utr (Do you have a UTR page - CARF-123)"
        )
      case None =>
        routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigateFromYourUniqueTaxpayerReference(userAnswers: UserAnswers): Call =
    userAnswers.get(OrganisationRegistrationTypePage) match {
      case Some(SoleTrader) => routes.PlaceholderController.onPageLoad("Must redirect to /your-name")
      case _ => routes.PlaceholderController.onPageLoad("Must redirect to /business-name")
    }

  private def navigateFromIsThisYourBusiness(userAnswers: UserAnswers): Call =
    userAnswers.get(IsThisYourBusinessPage) match {
      case Some(true) =>
        // User selects yes
        if (isSoleTrader(userAnswers)) {
          routes.PlaceholderController.onPageLoad("Must redirect to /register/individual-email (CARF-183)")
        } else {
          routes.PlaceholderController.onPageLoad("Must redirect to /register/your-contact-details (CARF-177)")
        }

      case Some(false) =>
        // User selects no
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

  private def isSoleTrader(userAnswers: UserAnswers): Boolean =
    userAnswers.get(OrganisationRegistrationTypePage) match {
      case Some(OrganisationRegistrationType.SoleTrader) => true
      case _ => false
    }

  private def isCTAutomatched(userAnswers: UserAnswers): Boolean =
    userAnswers.get(AutoMatchedUTRPage).isDefined

  private def navigateFromHaveNiNumber(userAnswers: UserAnswers): Call =
    userAnswers.get(HaveNiNumberPage) match {
      case Some(true) => // User selects yes
        routes.PlaceholderController.onPageLoad("Must redirect to /ni-number (CARF-164)")
      case Some(false) => // User selects no
        routes.PlaceholderController.onPageLoad("Must redirect to /without-id/name (CARF-169)")
      case None => routes.JourneyRecoveryController.onPageLoad()
    }
}