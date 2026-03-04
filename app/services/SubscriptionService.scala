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
import models.JourneyType.*
import models.error.ApiError
import models.error.ApiError.{AlreadyRegisteredError, InternalServerError}
import models.requests.{RegisterIndividualWithIdRequest, RegisterOrganisationWithIdRequest}
import models.responses.RegisterOrganisationWithIdResponse
import models.{BusinessDetails, IndividualDetails, IndividualRegistrationType, Name, OrganisationRegistrationType, UserAnswers}
import pages.*
import pages.individual.NiNumberPage
import pages.individualWithoutId.IndWithoutNinoNamePage
import pages.orgWithoutId.OrgWithoutIdBusinessNamePage
import pages.organisation.UniqueTaxpayerReferenceInUserAnswers
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionService @Inject() extends Logging {

  def subscribe(userAnswers: UserAnswers): Future[Either[ApiError, String]] = {
    // For testing success and error scenarios
    val journeyDifferentiator: String = {
      val ref = userAnswers.journeyType match {
        case Some(IndWithUtr) | Some(OrgWithUtr) =>
          userAnswers.get(UniqueTaxpayerReferenceInUserAnswers).map(_.uniqueTaxPayerReference)
        case Some(OrgWithoutId)                  => userAnswers.get(OrgWithoutIdBusinessNamePage).map(_.toUpperCase)
        case Some(IndWithNino)                   => userAnswers.get(NiNumberPage)
        case Some(IndWithoutId)                  => userAnswers.get(IndWithoutNinoNamePage).map(_.firstName.toUpperCase)
        case None                                => Some("1")
      }
      ref.getOrElse("1").take(1)
    }

    journeyDifferentiator match {
      case "2" | "B" =>
        Future.successful(Left(InternalServerError))
      case "5" | "Z" =>
        Future.successful(Left(AlreadyRegisteredError))
      case _         =>
        Future.successful(Right("Stub success!"))
    }
  }
}
