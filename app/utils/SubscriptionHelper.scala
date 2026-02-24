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

package utils

import models.JourneyType.*
import models.requests.{ContactInformation, CreateSubscriptionRequest, IndividualDetails, OrganisationDetails}
import models.{IdentifierType, JourneyType, Name, UserAnswers}
import pages.*
import pages.individual.{IndividualEmailPage, IndividualPhoneNumberPage, NiNumberPage, WhatIsYourNameIndividualPage}
import pages.individualWithoutId.{IndFindAddressPage, IndWithoutIdAddressPagePrePop, IndWithoutNinoNamePage}
import pages.orgWithoutId.TradingNamePage
import pages.organisation.*

class SubscriptionHelper {

  def buildSubscriptionRequest(userAnswers: UserAnswers): Option[CreateSubscriptionRequest] =
    for {
      safeId          <- userAnswers.get(SafeIdPage).map(_.value)
      primaryContact  <- buildPrimaryContact(userAnswers)
      tradingName      = getTradingName(userAnswers)
      gbUser           = isGBUser(userAnswers)
      secondaryContact = buildSecondaryContact(userAnswers)
    } yield CreateSubscriptionRequest(
      idType = IdentifierType.SAFE,
      idNumber = safeId,
      tradingName = tradingName,
      gbUser = gbUser,
      primaryContact = primaryContact,
      secondaryContact = secondaryContact
    )

  private def buildPrimaryContact(userAnswers: UserAnswers): Option[ContactInformation] =
    userAnswers.journeyType match {
      case Some(IndWithNino)                     => buildIndividualContact(userAnswers, WhatIsYourNameIndividualPage)
      case Some(IndWithoutId)                    => buildIndividualContact(userAnswers, IndWithoutNinoNamePage)
      case Some(IndWithUtr)                      => buildIndividualContact(userAnswers, WhatIsYourNamePage)
      case Some(OrgWithUtr) | Some(OrgWithoutId) => buildOrganisationPrimaryContact(userAnswers)
      case _                                     => None
    }

  private def buildIndividualContact(
      userAnswers: UserAnswers,
      namePage: QuestionPage[Name]
  ): Option[ContactInformation] =
    for {
      name  <- userAnswers.get(namePage)
      email <- userAnswers.get(IndividualEmailPage)
    } yield ContactInformation(
      individual = Some(IndividualDetails(name.firstName, name.lastName)),
      organisation = None,
      email = email,
      phone = userAnswers.get(IndividualPhoneNumberPage)
    )

  private def buildOrganisationPrimaryContact(userAnswers: UserAnswers): Option[ContactInformation] =
    for {
      name  <- userAnswers.get(FirstContactNamePage)
      email <- userAnswers.get(FirstContactEmailPage)
    } yield ContactInformation(
      individual = None,
      organisation = Some(OrganisationDetails(name)),
      email = email,
      phone = userAnswers.get(FirstContactPhoneNumberPage)
    )

  private def buildOrganisationSecondaryContact(userAnswers: UserAnswers): Option[ContactInformation] =
    for {
      name  <- userAnswers.get(OrganisationSecondContactNamePage)
      email <- userAnswers.get(OrganisationSecondContactEmailPage)
    } yield ContactInformation(
      individual = None,
      organisation = Some(OrganisationDetails(name)),
      email = email,
      phone = userAnswers.get(OrganisationSecondContactPhoneNumberPage)
    )

  private def buildSecondaryContact(userAnswers: UserAnswers): Option[ContactInformation] =
    for {
      journeyType      <- userAnswers.journeyType
      if isOrganisationJourney(journeyType)
      hasSecond        <- userAnswers.get(OrganisationHaveSecondContactPage)
      if hasSecond
      secondaryContact <- buildOrganisationSecondaryContact(userAnswers)
    } yield secondaryContact

  private def isOrganisationJourney(journeyType: JourneyType): Boolean =
    journeyType match {
      case OrgWithUtr | OrgWithoutId => true
      case _                         => false
    }

  private def getTradingName(userAnswers: UserAnswers): Option[String] =
    userAnswers.journeyType.flatMap {
      case OrgWithoutId => userAnswers.get(TradingNamePage)
      case OrgWithUtr   => userAnswers.get(WhatIsTheNameOfYourBusinessPage)
      case _            => None
    }

  private def isGBUser(userAnswers: UserAnswers): Boolean = {
    val businessHasUtr              = checkBusinessHasUtr(userAnswers)
    val individualHasNino           = userAnswers.get(NiNumberPage).exists(_.trim.nonEmpty)
    val individualAddressLookupIsGb = userAnswers.get(IndFindAddressPage).isDefined
    val individualManualAddressIsGb = checkIndividualManualAddressIsGb(userAnswers)
    val registeredAddressIsGb       = userAnswers.get(RegisteredAddressInUkPage).contains(true)

    businessHasUtr || individualHasNino || individualAddressLookupIsGb || individualManualAddressIsGb || registeredAddressIsGb
  }

  private def checkBusinessHasUtr(userAnswers: UserAnswers): Boolean =
    userAnswers.get(UniqueTaxpayerReferenceInUserAnswers).exists(_.uniqueTaxPayerReference.trim.nonEmpty)

  private def checkIndividualManualAddressIsGb(userAnswers: UserAnswers): Boolean =
    userAnswers.get(IndWithoutIdAddressPagePrePop).exists(_.countryUk.code == "GB")

}
