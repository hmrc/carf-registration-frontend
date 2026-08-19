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

import cats.syntax.all.*
import models.JourneyType.*
import models.audit.*
import models.error.ApiError.InternalServerError
import models.error.CarfError
import models.{JourneyType, RegistrationType, UserAnswers}
import pages.*
import pages.changeContactDetails.*
import pages.individual.*
import pages.individualWithoutId.*
import pages.orgWithoutId.*
import pages.organisation.*
import play.api.libs.json.{JsValue, Json}
import types.ResultT
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.*
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import utils.LoggerUtil.*

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class AuditService @Inject (auditConnector: AuditConnector)(using ec: ExecutionContext) {

  private case class AddressHolder(
      addressLine1: String,
      addressLine2: Option[String],
      addressLine3: Option[String],
      townOrCity: String,
      region: Option[String],
      postcode: Option[String],
      country: String
  )

  def auditRegistration(
      userAnswers: UserAnswers,
      journeyType: JourneyType,
      affinityGroup: AffinityGroup
  )(implicit hc: HeaderCarrier): ResultT[Unit] =
    for {
      registrationEvent <- ResultT.fromValue(
                             RegistrationAuditEvent(
                               affinityGroup = affinityGroup,
                               registeredAs = userAnswers.get(RegistrationTypePage).map(_.humanReadable),
                               registeredUkAddress = userAnswers
                                 .get(RegisteredAddressInUkPage)
                                 .fold(userAnswers.get(WhereDoYouLivePage))(Some(_)),
                               hasUtr = userAnswers.get(HaveUTRPage),
                               hasNINO = userAnswers.get(HaveNiNumberPage),
                               soleTraderWithUTRJourney = if (journeyType == IndWithUtr) {
                                 getUtrJourneyType(userAnswers)
                               } else None,
                               organisationWithIdJourney = if (journeyType == OrgWithUtr) {
                                 getOrganisationWithIdJourney(userAnswers)
                               } else None,
                               organisationWithoutIdJourney = if (journeyType == OrgWithoutId) {
                                 getOrganisationWithoutIdJourney(userAnswers)
                               } else None,
                               withNinoJourney = if (journeyType == IndWithNino) {
                                 getWithNinoJourney(userAnswers)
                               } else None,
                               individualWithoutIdJourney = if (journeyType == IndWithoutId) {
                                 getIndividualWithoutIdJourney(userAnswers)
                               } else None,
                               individualContactDetails = journeyType match {
                                 case IndWithNino | IndWithoutId | IndWithUtr =>
                                   getIndividualContactDetails(userAnswers)
                                 case OrgWithUtr | OrgWithoutId               => None
                               },
                               organisationContactDetails = journeyType match {
                                 case OrgWithUtr | OrgWithoutId               =>
                                   getOrganisationContactDetails(userAnswers)
                                 case IndWithNino | IndWithoutId | IndWithUtr => None
                               }
                             )
                           )
      extendedEvent      = convertToExtendedEvent(registrationEvent.toJson, "Registration")
      _                 <- sendEvent(extendedEvent, "Registration", journeyType.toString)
    } yield ()

  def auditChangeContactDetails(
      userAnswers: UserAnswers,
      isIndividual: Boolean
  )(implicit hc: HeaderCarrier): ResultT[Unit] =
    for {
      changeContactDetailsEvent <- ResultT.fromValue(
                                     if (isIndividual) {
                                       ChangeContactDetailsAuditEvent(
                                         individualUpdatedValues = getIndividualUpdatedValues(userAnswers),
                                         individualOriginalValues = getIndividualOriginalValues(userAnswers),
                                         organisationOriginalValues = None,
                                         organisationUpdatedValues = None
                                       )
                                     } else {
                                       ChangeContactDetailsAuditEvent(
                                         individualUpdatedValues = None,
                                         individualOriginalValues = None,
                                         organisationOriginalValues = getOrganisationOriginalValues(userAnswers),
                                         organisationUpdatedValues = getOrganisationUpdatedValues(userAnswers)
                                       )
                                     }
                                   )
      extendedEvent              = convertToExtendedEvent(Json.toJson(changeContactDetailsEvent), "ChangeContactDetails")
      _                         <- if (isIndividual) { sendEvent(extendedEvent, "ChangeContactDetails", "Individual") }
                                   else sendEvent(extendedEvent, "ChangeContactDetails", "Organisation")

    } yield ()

  private def convertToExtendedEvent(eventJsValue: JsValue, auditType: String) =
    ExtendedDataEvent(
      auditSource = "carf-registration-frontend",
      auditType = auditType,
      detail = eventJsValue
    )

  private def sendEvent(extendedEvent: ExtendedDataEvent, auditType: String, journeyType: String): ResultT[Unit] =
    ResultT.fromFuture(auditConnector.sendExtendedEvent(extendedEvent).map {
      case Success         =>
        logDebug(s"Successfully sent $auditType audit event for $journeyType")
        Right[CarfError, Unit](())
      case Disabled        =>
        logError(s"Failed to audit $auditType for $journeyType Disabled result returned")
        Left[CarfError, Unit](InternalServerError)
      case Failure(msg, _) =>
        logError(s"Failed to audit $auditType for $journeyType with message $msg")
        Left[CarfError, Unit](InternalServerError)
    } recover {
      case e if NonFatal(e) =>
        logError(s"Failed to audit $auditType for $journeyType")
        Left[CarfError, Unit](InternalServerError)
    })

  private def getUtrJourneyType(userAnswers: UserAnswers): Option[UtrJourneyAuditEvent] =
    (
      userAnswers.get(UniqueTaxpayerReferenceInUserAnswers),
      userAnswers.get(WhatIsYourNamePage),
      userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer)
    ).mapN { (utr, name, isThisYourBusiness) =>
      UtrJourneyAuditEvent(utr.uniqueTaxPayerReference, name.firstName, name.lastName, true)
    }

  private def getOrganisationWithIdJourney(userAnswers: UserAnswers): Option[OrganisationWithIdJourney] =
    userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer).map { isThisYouBusiness =>
      OrganisationWithIdJourney(
        userAnswers.get(UniqueTaxpayerReferenceInUserAnswers).map(_.uniqueTaxPayerReference),
        userAnswers.get(WhatIsTheNameOfYourBusinessPage),
        isThisYouBusiness
      )
    }

  private def getOrganisationWithoutIdJourney(userAnswers: UserAnswers): Option[OrganisationWithoutIdJourney] =
    (
      userAnswers.get(OrgWithoutIdBusinessNamePage),
      userAnswers.get(HaveTradingNamePage),
      userAnswers.get(OrganisationBusinessAddressPage)
    ).mapN { (businessName, haveTradingName, businessAddress) =>
      OrganisationWithoutIdJourney(
        businessName,
        haveTradingName,
        userAnswers.get(TradingNamePage),
        businessAddress.addressLine1,
        businessAddress.addressLine2,
        businessAddress.townOrCity,
        businessAddress.region,
        businessAddress.postcode,
        businessAddress.country.description
      )
    }

  private def getWithNinoJourney(userAnswers: UserAnswers): Option[WithNinoJourney] =
    (
      userAnswers.get(NiNumberPage),
      userAnswers.get(WhatIsYourNameIndividualPage),
      userAnswers.get(RegisterDateOfBirthPage)
    ).mapN { (nino, name, dob) =>
      val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      WithNinoJourney(
        nino,
        name.firstName,
        name.lastName,
        dob.format(dateFormatter)
      )
    }

  private def getIndividualWithoutIdJourney(userAnswers: UserAnswers): Option[IndividualWithoutIdJourney] = {
    val addressMaybe: Option[AddressHolder] = userAnswers
      .get(IndWithoutIdAddressNonUkPage)
      .fold(
        userAnswers.get(IndWithoutIdUkAddressInUserAnswers).map { addressUk =>
          AddressHolder(
            addressUk.addressLine1,
            addressUk.addressLine2,
            addressUk.addressLine3,
            addressUk.townOrCity,
            None,
            Some(addressUk.postCode),
            addressUk.countryUk.name
          )
        }
      ) { address =>
        Some(
          AddressHolder(
            address.addressLine1,
            address.addressLine2,
            None,
            address.townOrCity,
            address.region,
            address.postcode,
            address.country.description
          )
        )
      }

    (
      userAnswers.get(IndWithoutNinoNamePage),
      userAnswers.get(IndWithoutIdDateOfBirthPage),
      userAnswers.get(WhereDoYouLivePage),
      addressMaybe
    ).mapN { (name, dob, ukResident, address) =>
      val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      IndividualWithoutIdJourney(
        firstName = name.firstName,
        lastName = name.lastName,
        dateOfBirth = dob.format(dateFormatter),
        residentOfUkOrCrownDependency = ukResident,
        findYourAddress = userAnswers.get(IndFindAddressPage).map(_.postcode),
        propertyNameOrNumber = userAnswers.get(IndFindAddressPage).flatMap(_.propertyNameOrNumber),
        chooseYourAddress = userAnswers.get(IndWithoutIdChooseAddressPage),
        UPRN = userAnswers.get(AddressUPRNUserAnswers).map(_.toString),
        address.addressLine1,
        address.addressLine2,
        address.addressLine3,
        address.townOrCity,
        address.region,
        address.postcode,
        address.country
      )
    }
  }

  private def getIndividualContactDetails(userAnswers: UserAnswers): Option[IndividualContactDetails] =
    (
      userAnswers.get(IndividualEmailPage),
      userAnswers.get(IndividualHavePhonePage)
    ).mapN { (email, havePhone) =>
      IndividualContactDetails(
        email,
        havePhone,
        userAnswers.get(IndividualPhoneNumberPage)
      )
    }

  private def getOrganisationContactDetails(userAnswers: UserAnswers): Option[OrganisationContactDetails] =
    (
      userAnswers.get(FirstContactNamePage),
      userAnswers.get(FirstContactEmailPage),
      userAnswers.get(FirstContactPhonePage),
      userAnswers.get(OrganisationHaveSecondContactPage)
    ).mapN { (name, email, havePhone, secondContact) =>
      OrganisationContactDetails(
        name,
        email,
        havePhone,
        userAnswers.get(FirstContactPhoneNumberPage),
        secondContact,
        userAnswers.get(OrganisationSecondContactNamePage),
        userAnswers.get(OrganisationSecondContactEmailPage),
        userAnswers.get(OrganisationSecondContactHavePhonePage),
        userAnswers.get(OrganisationSecondContactPhoneNumberPage)
      )
    }

  private def getOrganisationOriginalValues(userAnswers: UserAnswers): Option[OrganisationValues] =
    userAnswers.displaySubscriptionResponse.flatMap(response =>
      response.success.carfSubscriptionDetails.primaryContact.organisation.map(primaryContact =>
        OrganisationValues(
          contact1Name = primaryContact.name,
          contact1EmailAddress = response.success.carfSubscriptionDetails.primaryContact.email,
          contact1ByPhone = response.success.carfSubscriptionDetails.primaryContact.phone.isDefined,
          contact1PhoneNumber = response.success.carfSubscriptionDetails.primaryContact.phone,
          contact2 = response.success.carfSubscriptionDetails.secondaryContact.isDefined,
          contact2Name = response.success.carfSubscriptionDetails.secondaryContact.flatMap(_.organisation.map(_.name)),
          contact2EmailAddress = response.success.carfSubscriptionDetails.secondaryContact.map(_.email),
          contact2ByPhone = if (response.success.carfSubscriptionDetails.secondaryContact.isDefined) {
            response.success.carfSubscriptionDetails.secondaryContact.map(_.phone.isDefined)
          } else None,
          contact2PhoneNumber = response.success.carfSubscriptionDetails.secondaryContact.flatMap(_.phone)
        )
      )
    )

  private def getOrganisationUpdatedValues(userAnswers: UserAnswers): Option[OrganisationValues] =
    (
      userAnswers.get(ChangeDetailsOrgFirstNamePage),
      userAnswers.get(ChangeDetailsOrgFirstEmailPage),
      userAnswers.get(ChangeDetailsOrgFirstHavePhonePage),
      userAnswers.get(ChangeDetailsOrgHaveSecondContactPage)
    ).mapN { (name, email, havePhone, secondContact) =>
      OrganisationValues(
        contact1Name = name,
        contact1EmailAddress = email,
        contact1ByPhone = havePhone,
        contact1PhoneNumber = if (havePhone) {
          userAnswers.get(ChangeDetailsOrgFirstPhoneNumberPage)
        } else None,
        contact2 = secondContact,
        contact2Name = if (secondContact) {
          userAnswers.get(ChangeDetailsOrgSecondNamePage)
        } else None,
        contact2EmailAddress = if (secondContact) {
          userAnswers.get(ChangeDetailsOrgSecondEmailPage)
        } else None,
        contact2ByPhone = if (secondContact) {
          userAnswers.get(ChangeDetailsOrgSecondHavePhonePage)
        } else None,
        contact2PhoneNumber = if (secondContact & userAnswers.get(ChangeDetailsOrgSecondHavePhonePage).contains(true)) {
          userAnswers.get(ChangeDetailsOrgSecondPhoneNumberPage)
        } else None
      )
    }

  private def getIndividualOriginalValues(userAnswers: UserAnswers): Option[IndividualValues] =
    userAnswers.displaySubscriptionResponse.map(response =>
      IndividualValues(
        emailAddress = response.success.carfSubscriptionDetails.primaryContact.email,
        contactByPhone = response.success.carfSubscriptionDetails.primaryContact.phone.isDefined,
        phoneNumber = response.success.carfSubscriptionDetails.primaryContact.phone
      )
    )

  private def getIndividualUpdatedValues(userAnswers: UserAnswers): Option[IndividualValues] =
    (userAnswers.get(ChangeDetailsIndividualEmailPage), userAnswers.get(ChangeDetailsIndividualHavePhonePage)).mapN {
      (email, havePhone) =>
        IndividualValues(
          emailAddress = email,
          contactByPhone = havePhone,
          phoneNumber = if (havePhone) {
            userAnswers.get(ChangeDetailsIndividualPhoneNumberPage)
          } else None
        )
    }

}
