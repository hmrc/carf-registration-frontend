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
import models.{BusinessDetails, IndividualDetails, IndividualRegistrationType, Name, OrganisationRegistrationType, UserAnswers}
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
  ): Future[Either[ApiError, IndividualDetails]] =
    val request = RegisterIndividualWithIdRequest(
      requiresNameMatch = true,
      IDNumber = nino,
      IDType = "NINO",
      dateOfBirth = dob.toString,
      firstName = name.firstName,
      lastName = name.lastName
    )
    connector
      .individualWithNino(request)
      .value
      .flatMap {
        case Right(response) =>
          logger.info(
            s"RegistrationConnector Successfully retrieved individual details for: $response.firstName, $response.lastName, safeId=$response.safeId"
          )
          Future.successful(
            Right(
              IndividualDetails(
                safeId = response.safeId,
                firstName = response.firstName,
                lastName = response.lastName,
                middleName = response.middleName,
                address = response.address
              )
            )
          )
        case Left(error)     =>
          logger.warn(s"Failed to retrieve individual details: $error")
          Future.successful(Left(error))
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

  // update in CARF-322
  def getBusinessWithUserInput(
      userAnswers: UserAnswers
  )(implicit hc: HeaderCarrier): Future[Option[BusinessDetails]] = {
    val registrationData = for {
      utr          <- userAnswers.get(YourUniqueTaxpayerReferencePage)
      businessName <- userAnswers.get(WhatIsTheNameOfYourBusinessPage) match {
                        case Some(value) => Some(value)
                        case None        => userAnswers.get(WhatIsYourNamePage).map(_.fullName)
                      }
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
        handleRegistrationResponse(connector.organisationWithUtr(request).value)
      case None                               =>
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
