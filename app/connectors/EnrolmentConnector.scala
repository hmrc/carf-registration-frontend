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
import models.error.ApiError
import models.error.ApiError.InternalServerError
import models.requests.EnrolmentRequest
import models.requests.EnrolmentRequest.*
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import types.ResultT
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EnrolmentConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2)(implicit
    ec: ExecutionContext
) extends Logging {

  private val enrolmentUrl = config.taxEnrolmentBaseUrl

  def createEnrolment(requestBody: EnrolmentRequest)(implicit hc: HeaderCarrier): ResultT[Unit] =
    EitherT {
      http
        .put(url"$enrolmentUrl")
        .withBody(Json.toJson(requestBody))
        .execute[HttpResponse]
        .map {
          case response if response.status == NO_CONTENT  => Right(())
          case response if response.status == BAD_REQUEST =>
            logger.error(s"Failed to create enrolment as Bad request was returned with message: ${response.body}")
            Left(ApiError.BadRequestError)
          case response                                   =>
            logger.error(s"Failed to create enrolment due to ${response.body}")
            Left(ApiError.InternalServerError)
        }.recover { case NonFatal(e) =>
          logger.error(s"Future Failed to complete due to: ${e.getMessage}")
          Left(InternalServerError)
        }
    }
}
