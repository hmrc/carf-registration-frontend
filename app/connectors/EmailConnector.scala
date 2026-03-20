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

import com.google.inject.Inject
import config.FrontendAppConfig
import models.SendEmailRequest
import play.api.Logging
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

sealed trait EmailStatus

case object EmailSent extends EmailStatus

case object EmailNotSent extends EmailStatus

class EmailConnector @Inject() (
    appConfig: FrontendAppConfig,
    httpV2Client: HttpClientV2
) extends Logging {

  def sendEmail(
      emailAddress: String,
      templateName: String,
      templateParams: Map[String, String]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[EmailStatus] =
    httpV2Client
      .post(url"${appConfig.emailUrl}")
      .withBody(
        Json.toJson(
          SendEmailRequest(
            to = List(emailAddress),
            templateId = templateName,
            parameters = templateParams,
            force = appConfig.emailSendForce
          )
        )
      )
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case ACCEPTED =>
            logger.info(s"Email sent successfully")
            EmailSent
          case status   =>
            logger.warn(s"Sending Email failed with response status $status")
            EmailNotSent
        }
      }
      .recoverWith { case t: Throwable =>
        logger.warn(s"Unable to connect to Email Service", t)
        Future.successful(EmailNotSent)
      }
}
