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

package connectors

import cats.data.EitherT
import config.FrontendAppConfig
import models.SubscriptionId
import models.error.ApiError
import models.error.ApiError.{AlreadyRegisteredError, InternalServerError, UnableToProcessSubscriptionError}
import models.requests.SubscriptionRequest
import models.responses.{DisplaySubscriptionResponse, SubscriptionResponse}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import types.ResultT
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2) extends Logging {

  def createSubscription(
      createSubscriptionRequest: SubscriptionRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubscriptionId] = {

    val submissionUrl = url"${config.carfRegistrationBaseUrl}/subscription/subscribe"

    processSubscriptionRequest(createSubscriptionRequest, submissionUrl, false)
  }

  def updateSubscription(
      updateSubscriptionRequest: SubscriptionRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[SubscriptionId] = {

    val submissionUrl = url"${config.carfRegistrationBaseUrl}/subscription/amend"

    processSubscriptionRequest(updateSubscriptionRequest, submissionUrl, true)
  }

  private def processSubscriptionRequest(request: SubscriptionRequest, submissionUrl: URL, isUpdate: Boolean)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext
  ): ResultT[SubscriptionId] = {

    val action = if isUpdate then "Updating" else "Creating"

    val requestBuilder =
      if (isUpdate) {
        http.put(submissionUrl)
      } else {
        http.post(submissionUrl)
      }

    logger.debug(
      s"[SubscriptionConnector] $action subscription with request:\n ${Json.prettyPrint(Json.toJson(request))}"
    )

    ResultT.fromFuture(
      requestBuilder
        .withBody(Json.toJson(request))
        .execute[HttpResponse]
        .map {
          case response if response.status == OK =>
            Try(response.json.as[SubscriptionResponse]) match {
              case Success(data)      => Right(data.subscriptionId)
              case Failure(exception) =>
                logger.warn(s"Error parsing SubscriptionResponse with endpoint: $submissionUrl")
                Left(ApiError.JsonValidationError)
            }
          case response                          =>
            logger.warn(s"Unexpected response: status code: ${response.status}, from endpoint: $submissionUrl")
            Left(handleErrorResponse(response))
        }
        .recover { case NonFatal(e) =>
          logger.error(s"Future Failed to complete due to: ${e.getMessage}")
          Left(InternalServerError)
        }
    )
  }

  def displaySubscription(
      carfId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): ResultT[DisplaySubscriptionResponse] = {
    val baseUrl = url"${config.carfRegistrationBaseUrl}/subscription/display/$carfId"

    logger.debug(
      s"[SubscriptionConnector] Displaying subscription with ID: $carfId"
    )

    ResultT.fromFuture(
      http
        .get(baseUrl)
        .execute[HttpResponse]
        .map { httpResponse =>
          httpResponse.status match {
            case OK        =>
              Try(httpResponse.json.as[DisplaySubscriptionResponse]) match {
                case Success(data)      => Right(data)
                case Failure(exception) =>
                  logger.warn(s"Error parsing DisplaySubscriptionResponse with endpoint: ${baseUrl.toURI}")
                  Left(ApiError.JsonValidationError)
              }
            case NOT_FOUND =>
              logger.warn(
                s"No match could be found for this user: status code: ${httpResponse.status}, from endpoint: ${baseUrl.toURI}"
              )
              Left(ApiError.NotFoundError)
            case _         =>
              logger.warn(s"Unexpected response: status code: ${httpResponse.status}, from endpoint: ${baseUrl.toURI}")
              Left(InternalServerError)
          }
        }
    )
  }

  private def handleErrorResponse(response: HttpResponse): ApiError = {
    val jsonBody = Try(Json.parse(response.body)).toOption.getOrElse(Json.obj())
    (jsonBody \ "status").asOpt[String] match {
      case Some("already_registered") =>
        logger.warn("Subscription already exists.")
        AlreadyRegisteredError

      case _ =>
        logger.warn(s"Received error response from backend: ${response.status}")
        UnableToProcessSubscriptionError
    }
  }

}
