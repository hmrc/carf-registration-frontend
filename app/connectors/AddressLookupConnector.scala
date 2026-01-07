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

import com.google.inject.Inject
import config.FrontendAppConfig
import models.requests.SearchByPostcodeRequest
import models.responses.AddressResponse
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupConnector @Inject() (val config: FrontendAppConfig, val http: HttpClientV2)(implicit
    ec: ExecutionContext
) extends Logging {

  private val searchByPostcodeUrl = url"${config.addressLookupBaseUrl}/lookup"

  def searchByPostcode(
      request: SearchByPostcodeRequest
  )(implicit hc: HeaderCarrier): Future[Seq[AddressResponse]] =
    http
      .post(searchByPostcodeUrl)
      .setHeader("X-Hmrc-Origin" -> "CARF")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap {
        case response if response.status equals OK =>
          Future.successful(
            response.json
              .as[Seq[AddressResponse]]
          )

        case response =>
          val message = s"Address Lookup failed with status ${response.status} Response body: ${response.body}"
          Future.failed(new HttpException(message, response.status))
      }
      .recover {
        case e: UpstreamErrorResponse =>
          logger.warn(
            s"[AddressLookupConnector] [searchByPostcode] - Upstream error: ${e.reportAs} message: ${e.getMessage}"
          )
          Nil
        case e                        =>
          logger.warn(s"[AddressLookupConnector] [searchByPostcode] - Error: ${e.getMessage}")
          Nil
      }

}
