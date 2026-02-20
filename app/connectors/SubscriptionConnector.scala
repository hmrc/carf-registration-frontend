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
import models.error.ApiError.{AlreadyRegisteredError, UnableToCreateEMTPSubscriptionError}
import models.requests.CreateSubscriptionRequest
import models.responses.CreateSubscriptionResponse
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
        .map { response =>
          if (is2xx(response.status)) {
            response.json.asOpt[CreateSubscriptionResponse] match {
              case Some(successResponse) => Right(successResponse.subscriptionId)
              case None                  => Left(UnableToCreateEMTPSubscriptionError)
            }
          } else {
            handleErrorResponse(response)
          }
        }
    }
  }

  private def handleErrorResponse(response: HttpResponse): Either[ApiError, SubscriptionId] = {
    val jsonBody = Try(Json.parse(response.body)).toOption.getOrElse(Json.obj())
    (jsonBody \ "status").asOpt[String] match {
      case Some("already_registered") =>
        logger.warn("Subscription already exists.")
        Left(AlreadyRegisteredError)

      case _ =>
        logger.warn(s"Received error response from backend: ${response.status}")
        Left(UnableToCreateEMTPSubscriptionError)
    }
  }

}
