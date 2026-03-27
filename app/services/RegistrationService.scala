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

package services

import connectors.RegistrationConnector
import models.JourneyType.{IndWithoutId, OrgWithoutId}
import models.error.ApiError.{ApplicationError, InternalServerError}
import models.error.{ApiError, CarfError, DataError}
import models.requests.*
import models.responses.{AddressRegistrationResponse, RegisterIndividualWithIdResponse, RegisterIndividualWithoutIdResponse, RegisterOrganisationWithIdResponse}
import models.{BusinessDetails, IndividualDetails, JourneyType, Name, OrganisationRegistrationType, SafeId, UserAnswers}
import pages.*
import pages.organisation.{RegistrationTypePage, UniqueTaxpayerReferenceInUserAnswers, WhatIsTheNameOfYourBusinessPage, WhatIsYourNamePage}
import play.api.Logging
import types.ResultT
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject() (connector: RegistrationConnector)(implicit ec: ExecutionContext) extends Logging {

  def registerForWithoutIdJourneys(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): ResultT[UserAnswers] =
    userAnswers.journeyType match {
      case Some(journeyType) if (journeyType == OrgWithoutId | journeyType == IndWithoutId) =>
        val result = if (journeyType == OrgWithoutId) {
          ???
        } else {
          registerIndWithoutId(userAnswers)
        }
        
        result.bimap(
          err =>
            logger.error(
              s"[RegistrationService] Failed to register without id. JourneyType: ${userAnswers.journeyType}"
            )
            InternalServerError
          ,
          success =>
            logger.info(
              s"[RegistrationService] Successfully registered user without id. JourneyType: ${userAnswers.journeyType}"
            )
            userAnswers.copy(safeId = Some(success))
        )
      case _                                       => ResultT.fromValue(userAnswers)
    }

  private def registerIndWithoutId(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): ResultT[SafeId] = {
    val request = ???
    
    val response = connector.individualWithNino(request)
  }

  def getIndividualByNino(nino: String, name: Name, dob: LocalDate)(implicit
      hc: HeaderCarrier
  ): Future[Either[CarfError, IndividualDetails]] = {
    val request = RegisterIndividualWithNinoRequest(
      requiresNameMatch = true,
      IDNumber = nino,
      IDType = "NINO",
      dateOfBirth = dob.toString,
      firstName = name.firstName,
      lastName = name.lastName
    )
    handleIndividualRegistrationResponse(connector.individualWithNino(request))
  }

  def getIndividualByUtr(
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier): Future[Either[CarfError, IndividualDetails]] = {
    val registrationData = for {
      utr  <- userAnswers.get(UniqueTaxpayerReferenceInUserAnswers)
      name <- userAnswers.get(WhatIsYourNamePage)
    } yield (utr, name)

    registrationData match {
      case Some((utr, name)) =>
        val request = RegisterIndividualWithUtrRequest(
          requiresNameMatch = true,
          IDNumber = utr.uniqueTaxPayerReference,
          IDType = "UTR",
          firstName = name.firstName,
          lastName = name.lastName
        )
        handleIndividualRegistrationResponse(connector.individualWithUtr(request))
      case None              =>
        logger.warn("Required Individual data was missing from UserAnswers.")
        Future.successful(Left(DataError))
    }
  }

  def getBusinessWithUtr(
      userAnswers: UserAnswers,
      utr: String
  )(implicit hc: HeaderCarrier): Future[Either[CarfError, BusinessDetails]] =
    if (!userAnswers.isCtAutoMatched) {
      val registrationData = for {
        businessName <- userAnswers.get(WhatIsTheNameOfYourBusinessPage)
        orgType      <- userAnswers.get(RegistrationTypePage)
      } yield (businessName, orgType.code)

      registrationData match {
        case Some((businessName, orgType)) =>
          val request = RegOrgWithIdNonAutoMatchRequest(
            requiresNameMatch = true,
            IDNumber = utr,
            IDType = "UTR",
            organisationName = businessName,
            organisationType = orgType
          )
          handleOrganisationRegistrationResponse(connector.organisationWithUtrNonAutoMatch(request))
        case None                          =>
          logger.warn("Required data was missing from UserAnswers.")
          Future.successful(Left(DataError))
      }
    } else {
      val request = RegOrgWithIdCTAutoMatchRequest(
        requiresNameMatch = false,
        IDNumber = utr,
        IDType = "UTR"
      )
      handleOrganisationRegistrationResponse(connector.organisationWithUtrCTAutoMatch(request))
    }

  private def handleOrganisationRegistrationResponse(
      responseFuture: ResultT[RegisterOrganisationWithIdResponse]
  ): Future[Either[ApiError, BusinessDetails]] =
    responseFuture.value.flatMap {
      case Right(response)       =>
        logger.info("Successfully retrieved organisation details.")
        Future.successful(
          Right(BusinessDetails(name = response.organisationName, address = response.address, safeId = response.safeId))
        )
      case Left(error: ApiError) =>
        logger.error(s"Failed to retrieve organisation details: $error")
        Future.successful(Left(error))
    }

  private def handleIndividualRegistrationResponse(
      responseFuture: ResultT[RegisterIndividualWithIdResponse]
  ): Future[Either[ApiError, IndividualDetails]] =
    responseFuture.value.flatMap {
      case Right(response)       =>
        logger.info("Successfully retrieved Individual details.")
        Future.successful(
          Right(
            IndividualDetails(
              safeId = response.safeId,
              firstName = response.firstName,
              middleName = response.middleName,
              lastName = response.lastName,
              address = response.address
            )
          )
        )
      case Left(error: ApiError) =>
        logger.error(s"Failed to retrieve Individual details: $error")
        Future.successful(Left(error))
    }

  def individualWithoutId(
      request: RegisterIndividualWithoutIdRequest
  )(implicit hc: HeaderCarrier): Future[Either[CarfError, IndividualDetails]] =
    handleIndividualWithoutIdRegistrationResponse(connector.individualWithoutId(request).value, request)

  private def handleIndividualWithoutIdRegistrationResponse(
      responseFuture: Future[Either[ApiError, RegisterIndividualWithoutIdResponse]],
      request: RegisterIndividualWithoutIdRequest
  ): Future[Either[ApiError, IndividualDetails]] =
    responseFuture.flatMap {
      case Right(response)       =>
        logger.info("Successfully retrieved Individual without ID details.")
        Future.successful(
          Right(
            IndividualDetails(
              safeId = response.safeId,
              firstName = request.firstName,
              middleName = None,
              lastName = request.lastName,
              address = mapAddressDetailsToAddress(request.address)
            )
          )
        )
      case Left(error: ApiError) =>
        logger.error(s"Failed to retrieve Individual without ID details: $error")
        Future.successful(Left(error))
    }

  private def mapAddressDetailsToAddress(addressDetails: AddressDetails): AddressRegistrationResponse =
    AddressRegistrationResponse(
      addressLine1 = addressDetails.addressLine1,
      addressLine2 = addressDetails.addressLine2,
      addressLine3 = addressDetails.addressLine3,
      addressLine4 = None,
      postalCode = addressDetails.postalCode,
      countryCode = addressDetails.countryCode,
      countryName = None
    )

}
