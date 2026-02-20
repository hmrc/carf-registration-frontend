/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import cats.implicits.*
import connectors.SubscriptionConnector
import models.JourneyType.*
import models.error.ApiError
import models.requests.{ContactInformation, CreateSubscriptionRequest, IndividualDetails, OrganisationDetails}
import models.{IdentifierType, JourneyType, Name, SubscriptionId, UserAnswers}
import pages.*
import pages.individual.{IndividualEmailPage, IndividualPhoneNumberPage, NiNumberPage, WhatIsYourNameIndividualPage}
import pages.individualWithoutId.{IndFindAddressPage, IndWithoutIdAddressPage, IndWithoutNinoNamePage}
import pages.orgWithoutId.TradingNamePage
import pages.organisation.*
import play.api.Logging
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionService @Inject() (subscriptionConnector: SubscriptionConnector) extends Logging {

  def subscribe(userAnswers: UserAnswers)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): Future[Either[ApiError, SubscriptionId]] =
    subscriptionConnector
      .createSubscription(buildSubscriptionRequest(userAnswers))
      .map { result =>
        logger.info(s"Subscription has been created successfully: $result")
        result
      }
      .leftMap { error =>
        logger.error(s"Failed to create subscription: $error")
        error
      }
      .value

  private def buildSubscriptionRequest(userAnswers: UserAnswers): CreateSubscriptionRequest = {
    val safeId = "safeid"

    CreateSubscriptionRequest(
      idType = IdentifierType.SAFE,
      idNumber = safeId,
      tradingName = getTradingName(userAnswers),
      gbUser = isGBUser(userAnswers),
      primaryContact = buildPrimaryContact(userAnswers),
      secondaryContact = buildSecondaryContact(userAnswers)
    )
  }

  private def buildPrimaryContact(userAnswers: UserAnswers): ContactInformation =
    userAnswers.journeyType match {
      case Some(IndWithNino)  => buildIndividualContact(userAnswers, WhatIsYourNameIndividualPage)
      case Some(IndWithoutId) => buildIndividualContact(userAnswers, IndWithoutNinoNamePage)
      case Some(IndWithUtr)   => buildIndividualContact(userAnswers, WhatIsYourNamePage)
      case _                  => buildOrganisationContact(userAnswers, isPrimary = true)
    }

  private def buildIndividualContact(
      userAnswers: UserAnswers,
      namePage: QuestionPage[Name]
  ): ContactInformation =
    ContactInformation(
      individual = userAnswers.get(namePage).map(name => IndividualDetails(name.firstName, name.lastName)),
      organisation = None,
      email = userAnswers.get(IndividualEmailPage).fold("")(identity),
      phone = userAnswers.get(IndividualPhoneNumberPage)
    )

  private def buildOrganisationContact(
      userAnswers: UserAnswers,
      isPrimary: Boolean
  ): ContactInformation =
    if (isPrimary) {
      ContactInformation(
        individual = None,
        organisation = userAnswers.get(FirstContactNamePage).map(OrganisationDetails(_)),
        email = userAnswers.get(FirstContactEmailPage).fold("")(identity),
        phone = userAnswers.get(FirstContactPhoneNumberPage)
      )
    } else {
      ContactInformation(
        individual = None,
        organisation = userAnswers.get(OrganisationSecondContactNamePage).map(OrganisationDetails(_)),
        email = userAnswers.get(OrganisationSecondContactEmailPage).fold("")(identity),
        phone = userAnswers.get(OrganisationSecondContactPhoneNumberPage)
      )
    }

  private def buildSecondaryContact(userAnswers: UserAnswers): Option[ContactInformation] =
    if (isOrganisationJourney(userAnswers.journeyType) && hasSecondContact(userAnswers)) {
      Some(buildOrganisationContact(userAnswers, isPrimary = false))
    } else {
      None
    }

  private def isOrganisationJourney(journeyType: Option[JourneyType]): Boolean =
    journeyType match {
      case Some(OrgWithUtr) | Some(OrgWithoutId) => true
      case _                                     => false
    }

  private def hasSecondContact(userAnswers: UserAnswers): Boolean =
    userAnswers.get(OrganisationHaveSecondContactPage).contains(true)

  private def getTradingName(userAnswers: UserAnswers): Option[String] =
    userAnswers.journeyType match {
      case Some(OrgWithoutId) => userAnswers.get(TradingNamePage)
      case Some(OrgWithUtr)   => userAnswers.get(WhatIsTheNameOfYourBusinessPage)
      case _                  => None
    }

  private def isGBUser(userAnswers: UserAnswers): Boolean = {
    val businessHasUtr              =
      userAnswers.get(UniqueTaxpayerReferenceInUserAnswers).exists(_.uniqueTaxPayerReference.trim.nonEmpty)
    val individualHasNino           = userAnswers.get(NiNumberPage).exists(_.trim.nonEmpty)
    val individualAddressLookupIsGb = userAnswers.get(IndFindAddressPage).nonEmpty
    val individualManualAddressIsGb = userAnswers.get(IndWithoutIdAddressPage).exists(_.countryCode == "GB")
    val registeredAddressIsGb       = userAnswers.get(RegisteredAddressInUkPage).contains(true)

    businessHasUtr || individualHasNino || individualAddressLookupIsGb || individualManualAddressIsGb || registeredAddressIsGb
  }

}
