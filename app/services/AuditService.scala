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
import models.audit.*
import models.error.ApiError.InternalServerError
import models.error.{CarfError, DataError}
import models.{JourneyType, RegistrationType, UserAnswers}
import pages.individual.*
import pages.individualWithoutId.*
import pages.orgWithoutId.*
import pages.organisation.*
import pages.{AddressUPRNUserAnswers, IsThisYourBusinessPage, RegisteredAddressInUkPage, WhereDoYouLivePage}
import play.api.Logging
import play.api.libs.json.JsValue
import types.ResultT
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.play.audit.http.connector
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.*
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class AuditService @Inject (auditConnector: AuditConnector)(using ec: ExecutionContext) extends Logging {

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
  ): ResultT[Unit] = {

    val hasUtr = userAnswers
      .get(HaveUTRPage)
      .fold {
        userAnswers.get(UniqueTaxpayerReferenceInUserAnswers).flatMap(_ => Some(true))
      }(Some(_))

    for {
      registrationEvent <- userAnswers
                             .get(RegistrationTypePage)
                             .fold(
                               ResultT.fromError[RegistrationAuditEvent](DataError)
                             ) { regType =>
                               ResultT.fromValue(
                                 RegistrationAuditEvent(
                                   affinityGroup = affinityGroup,
                                   registeredAs = regType,
                                   registeredUkAddress = userAnswers
                                     .get(RegisteredAddressInUkPage)
                                     .fold(userAnswers.get(WhereDoYouLivePage))(Some(_)),
                                   hasUtr = hasUtr,
                                   hasNINO = userAnswers.get(HaveNiNumberPage).fold(None)(Some(_)),
                                   withUtrJourney = getUtrJourneyType(userAnswers),
                                   organisationWithIdJourney = getOrganisationWithIdJourney(userAnswers),
                                   organisationWithoutIdJourney = getOrganisationWithoutIdJourney(userAnswers),
                                   withNinoJourney = getWithNinoJourney(userAnswers),
                                   individualWithoutIdJourney = getIndividualWithoutIdJourney(userAnswers),
                                   individualContactDetails = getIndividualContactDetails(userAnswers),
                                   organisationContactDetails = getOrganisationContactDetails(userAnswers)
                                 )
                               )
                             }
      extendedEvent      = convertToExtendedEvent(registrationEvent.toJson, "Registration")
      _                 <- ResultT.fromFuture(
                             auditConnector.sendExtendedEvent(extendedEvent).map {
                               case Success         =>
                                 logger.debug(s"Successfully sent Registration audit event for ${journeyType.toString}")
                                 Right[CarfError, Unit](())
                               case Disabled        =>
                                 logger.error(s"Failed to audit Registration for ${journeyType.toString} Disabled result returned")
                                 Left[CarfError, Unit](InternalServerError)
                               case Failure(msg, _) =>
                                 logger.error(s"Failed to audit Registration for ${journeyType.toString} with message $msg")
                                 Left[CarfError, Unit](InternalServerError)
                             } recover {
                               case e if NonFatal(e) =>
                                 logger.error(s"Failed to audit Registration for ${journeyType.toString}")
                                 Left[CarfError, Unit](InternalServerError)
                             }
                           )
    } yield ()
  }

  private def convertToExtendedEvent(eventJsValue: JsValue, auditType: String) =
    ExtendedDataEvent(
      auditSource = "carf-registration-frontend",
      auditType = auditType,
      detail = eventJsValue
    )

  private def getUtrJourneyType(userAnswers: UserAnswers): Option[UtrJourneyAuditEvent] =
    (
      userAnswers.get(UniqueTaxpayerReferenceInUserAnswers),
      userAnswers.get(WhatIsYourNamePage),
      userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer)
    ).mapN { (utr, name, isThisYourBusiness) =>
      UtrJourneyAuditEvent(utr.uniqueTaxPayerReference, name.firstName, name.lastName, isThisYourBusiness)
    }

  private def getOrganisationWithIdJourney(userAnswers: UserAnswers): Option[OrganisationWithIdJourney] =
    (
      userAnswers.get(UniqueTaxpayerReferenceInUserAnswers),
      userAnswers.get(WhatIsTheNameOfYourBusinessPage),
      userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer)
    ).mapN { (utr, businessName, isThisYouBusiness) =>
      OrganisationWithIdJourney(utr.uniqueTaxPayerReference, businessName, isThisYouBusiness)
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
        findYourAddress = userAnswers.get(IndFindAddressPage).isDefined,
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
}
