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
import com.google.inject.Inject
import config.FrontendAppConfig
import models.SubscriptionId
import models.error.ApiError
import models.error.ApiError.{AlreadyRegisteredError, UnableToCreateEMTPSubscriptionError}
import models.requests.*
import models.responses.CreateSubscriptionResponse
import play.api.Logging
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class SubscriptionConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2) extends Logging {

  def createSubscription(
      createSubscriptionRequest: CreateSubscriptionRequest
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, ApiError, SubscriptionId] = {

    val submissionUrl = url"${config.carfRegistrationBaseUrl}/subscription/subscribe"

    EitherT {
      http
        .post(submissionUrl)
        .withBody(Json.toJson(createSubscriptionRequest))
        .execute[HttpResponse]
        .map {
          case response if response.status == CREATED =>
            Try(response.json.as[CreateSubscriptionResponse]) match {
              case Success(data)      => Right(data.subscriptionId)
              case Failure(exception) =>
                logger.warn(s"Error parsing CreateSubscriptionResponse with endpoint: $submissionUrl")
                Left(ApiError.JsonValidationError)
            }
          case response                               =>
            logger.warn(s"Unexpected response: status code: ${response.status}, from endpoint: $submissionUrl")
            Left(handleErrorResponse(response))
        }
    }
  }

  private def handleErrorResponse(response: HttpResponse): ApiError = {
    val jsonBody = Try(Json.parse(response.body)).toOption.getOrElse(Json.obj())
    (jsonBody \ "status").asOpt[String] match {
      case Some("already_registered") =>
        logger.warn("Subscription already exists.")
        AlreadyRegisteredError

      case _ =>
        logger.warn(s"Received error response from backend: ${response.status}")
        UnableToCreateEMTPSubscriptionError
    }
  }

}
