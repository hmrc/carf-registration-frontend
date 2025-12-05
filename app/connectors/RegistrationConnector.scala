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

package connectors

import cats.data.EitherT
import com.google.inject.Inject
import config.FrontendAppConfig
import models.error.ApiError
import models.requests.{RegisterIndividualWithIdRequest, RegisterOrganisationWithIdRequest}
import models.responses.{RegisterIndividualWithIdResponse, RegisterOrganisationWithIdResponse}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class RegistrationConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2)(implicit
    ec: ExecutionContext
) extends Logging {

  private val backendBaseUrl = config.carfRegistrationBaseUrl

  def individualWithNino(
      request: RegisterIndividualWithIdRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegisterIndividualWithIdResponse] =
    registerIndividualWithId(request, url"$backendBaseUrl/individual/nino")

  def individualWithUtr(
      request: RegisterIndividualWithIdRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegisterIndividualWithIdResponse] =
    registerIndividualWithId(request, url"$backendBaseUrl/individual/utr")

  private def registerIndividualWithId(
      request: RegisterIndividualWithIdRequest,
      endpoint: URL
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegisterIndividualWithIdResponse] =
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .map {
          case response if response.status == OK        =>
            Try(response.json.as[RegisterIndividualWithIdResponse]) match {
              case Success(data)      =>
                Right(data)
              case Failure(exception) =>
                logger.warn(
                  s"Error parsing response as RegisterIndividualWithIdResponse with endpoint: ${endpoint.toURI}"
                )
                Left(ApiError.JsonValidationError)
            }
          case response if response.status == NOT_FOUND =>
            logger.warn(
              s"No match could be found for this user: status code: ${response.status}, from endpoint: ${endpoint.toURI}"
            )
            Left(ApiError.NotFoundError)
          case response                                 =>
            logger.warn(s"Unexpected response: status code: ${response.status}, from endpoint: ${endpoint.toURI}")
            Left(ApiError.InternalServerError)
        }
    }

  def organisationWithUtr(
      request: RegisterOrganisationWithIdRequest
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegisterOrganisationWithIdResponse] =
    registerOrganisationWithId(request, url"$backendBaseUrl/organisation/utr")

  private def registerOrganisationWithId(
      request: RegisterOrganisationWithIdRequest,
      endpoint: URL
  )(implicit hc: HeaderCarrier): EitherT[Future, ApiError, RegisterOrganisationWithIdResponse] =
    EitherT {
      http
        .post(endpoint)
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .map {
          case response if response.status == OK        =>
            Try(response.json.as[RegisterOrganisationWithIdResponse]) match {
              case Success(data)      => Right(data)
              case Failure(exception) =>
                logger.warn(s"Error parsing RegisterOrganisationWithIdResponse with endpoint: ${endpoint.toURI}")
                Left(ApiError.JsonValidationError)
            }
          case response if response.status == NOT_FOUND =>
            logger.warn(
              s"No match found for organisation: status code: ${response.status}, from endpoint: ${endpoint.toURI}"
            )
            Left(ApiError.NotFoundError)
          case response                                 =>
            logger.warn(
              s"Unexpected response for organisation: status code: ${response.status}, from endpoint: ${endpoint.toURI}"
            )
            Left(ApiError.InternalServerError)
        }
    }
}
