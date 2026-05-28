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
import models.requests.*
import models.responses.{DisplaySubscriptionIndividual, DisplaySubscriptionResponse}
import models.{IdentifierType, JourneyType, Name, UserAnswers}
import pages.*
import pages.changeContactDetails.*
import pages.individual.*
import pages.individualWithoutId.{IndFindAddressPage, IndWithoutIdAddressPagePrePop, IndWithoutNinoNamePage}
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, TradingNamePage}
import pages.organisation.*

class SubscriptionHelper {

  def buildSubscriptionRequest(userAnswers: UserAnswers): Option[SubscriptionRequest] =
    for {
      safeId          <- userAnswers.safeId
      primaryContact  <- buildPrimaryContact(userAnswers)
      tradingName      = getTradingName(userAnswers)
      gbUser           = isGBUser(userAnswers)
      secondaryContact = buildSecondaryContact(userAnswers)
    } yield SubscriptionRequest(
      idType = IdentifierType.SAFE,
      idNumber = safeId.value,
      tradingName = tradingName,
      gbUser = gbUser,
      primaryContact = primaryContact,
      secondaryContact = secondaryContact
    )

  def buildUpdatedSubscriptionRequest(userAnswers: UserAnswers): Option[SubscriptionRequest] =
    for {
      displayResponse  <- userAnswers.displaySubscriptionResponse
      isIndividual     <- displayResponse.isIndividualRegistrationType
      primaryContact   <- buildChangePrimaryContact(userAnswers, displayResponse)
      carfId            = displayResponse.success.carfSubscriptionDetails.carfReference
      tradingName       = displayResponse.success.carfSubscriptionDetails.tradingName
      gbUser            = displayResponse.success.carfSubscriptionDetails.gbUser
      secondaryContact <-
        if (isIndividual) {
          Some(None)
        } else {
          buildChangeSecondaryContact(userAnswers)
        }
    } yield SubscriptionRequest(
      idType = IdentifierType.ZCAR,
      idNumber = carfId,
      tradingName = tradingName,
      gbUser = gbUser,
      primaryContact = primaryContact,
      secondaryContact = secondaryContact
    )

  private def buildPrimaryContact(userAnswers: UserAnswers): Option[SubscriptionContactDetails] =
    userAnswers.journeyType match {
      case Some(IndWithNino)                     => buildIndividualContact(userAnswers, WhatIsYourNameIndividualPage)
      case Some(IndWithoutId)                    => buildIndividualContact(userAnswers, IndWithoutNinoNamePage)
      case Some(IndWithUtr)                      => buildIndividualContact(userAnswers, WhatIsYourNamePage)
      case Some(OrgWithUtr) | Some(OrgWithoutId) => buildOrganisationPrimaryContact(userAnswers)
      case _                                     => None
    }

  private def buildChangePrimaryContact(
      userAnswers: UserAnswers,
      displaySubscriptionResponse: DisplaySubscriptionResponse
  ): Option[SubscriptionContactDetails] =
    displaySubscriptionResponse.success.carfSubscriptionDetails.primaryContact.individual match {
      case Some(individual) => buildChangedIndividualContact(userAnswers, individual)
      case _                => buildChangedOrganisationPrimaryContact(userAnswers)
    }

  private def buildIndividualContact(
      userAnswers: UserAnswers,
      namePage: QuestionPage[Name]
  ): Option[SubscriptionContactDetails] =
    for {
      name      <- userAnswers.get(namePage)
      email     <- userAnswers.get(IndividualEmailPage)
      havePhone <- userAnswers.get(IndividualHavePhonePage)
      phone     <- if (havePhone) {
                     userAnswers.get(IndividualPhoneNumberPage).map(Some(_))
                   } else {
                     Some(None)
                   }
    } yield SubscriptionContactDetails(
      individual = Some(SubscriptionIndividualContact(name.firstName, name.lastName)),
      organisation = None,
      email = email,
      phone = phone
    )

  private def buildChangedIndividualContact(
      userAnswers: UserAnswers,
      individual: DisplaySubscriptionIndividual
  ): Option[SubscriptionContactDetails] =
    for {
      email     <- userAnswers.get(ChangeDetailsIndividualEmailPage)
      havePhone <- userAnswers.get(ChangeDetailsIndividualHavePhonePage)
      phone     <- if (havePhone) {
                     userAnswers.get(ChangeDetailsIndividualPhoneNumberPage).map(Some(_))
                   } else {
                     Some(None)
                   }
    } yield SubscriptionContactDetails(
      individual = Some(
        SubscriptionIndividualContact(
          individual.firstName,
          individual.lastName
        )
      ),
      organisation = None,
      email = email,
      phone = phone
    )

  private def buildOrganisationPrimaryContact(userAnswers: UserAnswers): Option[SubscriptionContactDetails] =
    for {
      name      <- userAnswers.get(FirstContactNamePage)
      email     <- userAnswers.get(FirstContactEmailPage)
      havePhone <- userAnswers.get(FirstContactPhonePage)
      phone     <- if (havePhone) {
                     userAnswers.get(FirstContactPhoneNumberPage).map(Some(_))
                   } else {
                     Some(None)
                   }
    } yield SubscriptionContactDetails(
      individual = None,
      organisation = Some(SubscriptionOrganisationContact(name)),
      email = email,
      phone = phone
    )

  private def buildChangedOrganisationPrimaryContact(userAnswers: UserAnswers): Option[SubscriptionContactDetails] =
    for {
      name      <- userAnswers.get(ChangeDetailsOrgFirstNamePage)
      email     <- userAnswers.get(ChangeDetailsOrgFirstEmailPage)
      havePhone <- userAnswers.get(ChangeDetailsOrgFirstHavePhonePage)
      phone     <- if (havePhone) {
                     userAnswers.get(ChangeDetailsOrgFirstPhoneNumberPage).map(Some(_))
                   } else {
                     Some(None)
                   }
    } yield SubscriptionContactDetails(
      individual = None,
      organisation = Some(SubscriptionOrganisationContact(name)),
      email = email,
      phone = phone
    )

  private def buildOrganisationSecondaryContact(userAnswers: UserAnswers): Option[SubscriptionContactDetails] =
    for {
      name      <- userAnswers.get(OrganisationSecondContactNamePage)
      email     <- userAnswers.get(OrganisationSecondContactEmailPage)
      havePhone <- userAnswers.get(OrganisationSecondContactHavePhonePage)
      phone     <- if (havePhone) {
                     userAnswers.get(OrganisationSecondContactPhoneNumberPage).map(Some(_))
                   } else {
                     Some(None)
                   }
    } yield SubscriptionContactDetails(
      individual = None,
      organisation = Some(SubscriptionOrganisationContact(name)),
      email = email,
      phone = phone
    )

  private def buildChangedOrganisationSecondaryContact(userAnswers: UserAnswers): Option[SubscriptionContactDetails] =
    for {
      name      <- userAnswers.get(ChangeDetailsOrgSecondNamePage)
      email     <- userAnswers.get(ChangeDetailsOrgSecondEmailPage)
      havePhone <- userAnswers.get(ChangeDetailsOrgSecondHavePhonePage)
      phone     <- if (havePhone) {
                     userAnswers.get(ChangeDetailsOrgSecondPhoneNumberPage).map(Some(_))
                   } else {
                     Some(None)
                   }
    } yield SubscriptionContactDetails(
      individual = None,
      organisation = Some(SubscriptionOrganisationContact(name)),
      email = email,
      phone = phone
    )

  private def buildSecondaryContact(userAnswers: UserAnswers): Option[SubscriptionContactDetails] =
    for {
      journeyType      <- userAnswers.journeyType
      if isOrganisationJourney(journeyType)
      hasSecond        <- userAnswers.get(OrganisationHaveSecondContactPage)
      if hasSecond
      secondaryContact <- buildOrganisationSecondaryContact(userAnswers)
    } yield secondaryContact

  private def buildChangeSecondaryContact(
      userAnswers: UserAnswers
  ): Option[Option[SubscriptionContactDetails]] =
    for {
      hasSecond        <- userAnswers.get(ChangeDetailsOrgHaveSecondContactPage)
      secondaryContact <-
        if (hasSecond) { buildChangedOrganisationSecondaryContact(userAnswers).map(Some(_)) }
        else {
          Some(None)
        }
    } yield secondaryContact

  private def isOrganisationJourney(journeyType: JourneyType): Boolean =
    journeyType match {
      case OrgWithUtr | OrgWithoutId => true
      case _                         => false
    }

  private def getTradingName(userAnswers: UserAnswers): Option[String] = {

    val businessNameAutoMatch: Option[String] =
      userAnswers.get(IsThisYourBusinessPage).map(_.businessDetails.name)

    if (userAnswers.isCtAutoMatched) {
      businessNameAutoMatch
    } else {
      userAnswers.journeyType.flatMap {
        case IndWithUtr   => businessNameAutoMatch
        case OrgWithoutId =>
          userAnswers
            .get(TradingNamePage)
            .filter(_ => userAnswers.get(HaveTradingNamePage).exists(identity))
            .orElse(userAnswers.get(OrgWithoutIdBusinessNamePage))

        case _ => userAnswers.get(WhatIsTheNameOfYourBusinessPage)
      }
    }
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
