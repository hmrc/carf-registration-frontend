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
import models.error.ApiError
import models.requests.{RegisterIndividualWithNinoRequest, RegisterIndividualWithUtrRequest, RegisterOrganisationWithIdRequest}
import models.responses.{RegisterIndividualWithIdResponse, RegisterOrganisationWithIdResponse}
import models.{BusinessDetails, IndividualDetails, Name, OrganisationRegistrationType, UserAnswers}
import pages.*
import pages.organisation.{RegistrationTypePage, WhatIsTheNameOfYourBusinessPage, WhatIsYourNamePage, YourUniqueTaxpayerReferencePage}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject() (connector: RegistrationConnector)(implicit ec: ExecutionContext) extends Logging {

  def getIndividualByNino(nino: String, name: Name, dob: LocalDate)(implicit
      hc: HeaderCarrier
  ): Future[Option[IndividualDetails]] = {
    val request = RegisterIndividualWithNinoRequest(
      requiresNameMatch = true,
      IDNumber = nino,
      IDType = "NINO",
      dateOfBirth = dob.toString,
      firstName = name.firstName,
      lastName = name.lastName
    )
    handleIndividualRegistrationResponse(connector.individualWithNino(request).value)
  }

  def getIndividualByUtr(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Option[IndividualDetails]] = {
    val registrationData = for {
      utr  <- userAnswers.get(YourUniqueTaxpayerReferencePage)
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
        handleIndividualRegistrationResponse(connector.individualWithUtr(request).value)
      case None              =>
        logger.warn("Required Individual data was missing from UserAnswers.")
        Future.successful(None)
    }
  }

  def getBusinessWithEnrolmentCtUtr(utr: String)(implicit hc: HeaderCarrier): Future[Option[BusinessDetails]] = {
    val request = RegisterOrganisationWithIdRequest(
      requiresNameMatch = false,
      IDNumber = utr,
      IDType = "UTR",
      organisationName = None,
      organisationType = None
    )
    handleOrganisationRegistrationResponse(connector.organisationWithUtr(request).value)
  }

  def getBusinessWithUserInput(
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier): Future[Option[BusinessDetails]] = {
    val registrationData = for {
      utr          <- userAnswers.get(YourUniqueTaxpayerReferencePage)
      businessName <- userAnswers.get(WhatIsTheNameOfYourBusinessPage)
      orgType      <- userAnswers.get(RegistrationTypePage)
    } yield (utr, businessName, orgType.code)

    registrationData match {
      case Some((utr, businessName, orgType)) =>
        val request = RegisterOrganisationWithIdRequest(
          requiresNameMatch = true,
          IDNumber = utr.uniqueTaxPayerReference,
          IDType = "UTR",
          organisationName = Some(businessName),
          organisationType = Some(orgType)
        )
        handleOrganisationRegistrationResponse(connector.organisationWithUtr(request).value)
      case None                               =>
        logger.warn("Required data was missing from UserAnswers.")
        Future.successful(None)
    }
  }

  private def handleOrganisationRegistrationResponse(
      responseFuture: Future[Either[ApiError, RegisterOrganisationWithIdResponse]]
  ): Future[Option[BusinessDetails]] =
    responseFuture.flatMap {
      case Right(response)              =>
        logger.info("Successfully retrieved organisation details.")
        Future.successful(
          Some(
            BusinessDetails(
              name = response.organisationName,
              address = response.address
            )
          )
        )
      case Left(ApiError.NotFoundError) =>
        logger.warn("Not Found (404) for organisation details.")
        Future.successful(None)
      case Left(error)                  =>
        logger.error(s"Failed to retrieve organisation details: $error")
        Future.failed(new Exception("Unexpected error!"))
    }

  private def handleIndividualRegistrationResponse(
      responseFuture: Future[Either[ApiError, RegisterIndividualWithIdResponse]]
  ): Future[Option[IndividualDetails]] =
    responseFuture.flatMap {
      case Right(response)              =>
        logger.info("Successfully retrieved Individual details.")
        Future.successful(
          Some(
            IndividualDetails(
              safeId = response.safeId,
              firstName = response.firstName,
              middleName = response.middleName,
              lastName = response.lastName,
              address = response.address
            )
          )
        )
      case Left(ApiError.NotFoundError) =>
        logger.warn("Not Found (404) for Individual details.")
        Future.successful(None)
      case Left(error)                  =>
        logger.error(s"Failed to retrieve Individual details: $error")
        Future.failed(new Exception("Unexpected error!"))
    }
}
