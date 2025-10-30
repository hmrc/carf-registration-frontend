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
import models.requests.{RegisterIndividualWithIdRequest, RegisterOrganisationWithIdRequest}
import models.responses.RegisterOrganisationWithIdResponse
import models.{BusinessDetails, IndividualDetails, OrganisationRegistrationType, UserAnswers}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.Logging
import pages.{OrganisationRegistrationTypePage, WhatIsTheNameOfYourBusinessPage, YourUniqueTaxpayerReferencePage}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject() (connector: RegistrationConnector)(implicit ec: ExecutionContext) extends Logging {

  def getIndividualByNino(ninoProxy: String)(implicit hc: HeaderCarrier): Future[Option[IndividualDetails]] =
    connector
      .individualWithNino(
        request = RegisterIndividualWithIdRequest(
          requiresNameMatch = true,
          // TODO: Replace it with actual NINO CARF-166
          IDNumber = ninoProxy,
          IDType = "NINO",
          dateOfBirth = "test-dob",
          firstName = "john",
          lastName = "doe"
        )
      )
      .value
      .flatMap {
        case Right(response)              =>
          logger.info("Successfully retrieved individual details.")
          Future.successful(
            Some(
              IndividualDetails(
                safeId = response.safeId,
                firstName = response.firstName,
                lastName = response.lastName,
                middleName = response.middleName,
                address = response.address
              )
            )
          )
        case Left(ApiError.NotFoundError) =>
          logger.warn("Not Found (404) for individual details.")
          Future.successful(None)
        case Left(error)                  =>
          Future.failed(new Exception("Unexpected Error!"))
      }

  def getBusinessWithEnrolmentCtUtr(utr: String)(implicit hc: HeaderCarrier): Future[Option[BusinessDetails]] = {
    val request = RegisterOrganisationWithIdRequest(
      requiresNameMatch = false,
      IDNumber = utr,
      IDType = "UTR",
      organisationName = None,
      organisationType = None
    )

    handleRegistrationResponse(connector.organisationWithUtr(request).value)
  }

  def getBusinessWithUserInput(
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier): Future[Option[BusinessDetails]] = {
    val registrationData = for {
      utr          <- userAnswers.get(YourUniqueTaxpayerReferencePage)
      businessName <- userAnswers.get(WhatIsTheNameOfYourBusinessPage)
      orgType      <- userAnswers.get(OrganisationRegistrationTypePage)
    } yield (utr, businessName, orgType)

    registrationData match {
      case Some((utr, businessName, orgType)) =>
        val request = RegisterOrganisationWithIdRequest(
          requiresNameMatch = true,
          IDNumber = utr.uniqueTaxPayerReference,
          IDType = "UTR",
          organisationName = Some(businessName),
          organisationType = Some(orgType.code)
        )
        handleRegistrationResponse(connector.organisationWithUtr(request).value)

      case None =>
        logger.warn("Required data was missing from UserAnswers.")
        Future.successful(None)
    }
  }

  private def handleRegistrationResponse(
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

      case Left(error) =>
        logger.error(s"Failed to retrieve organisation details: $error")
        Future.failed(new Exception("Unexpected error!"))
    }
}
