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

package services

import javax.inject.{Inject, Singleton}
import play.api.Logging
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject() ()(implicit ec: ExecutionContext) extends Logging {

  /** Sends a registration confirmation email to the provided addresses.
    *
    * @param emails
    *   List of email addresses to notify
    * @param subscriptionId
    *   The CARF User ID (subscription ID) to include in the email content
    * @param idNumberOpt
    *   Optional UTR or NINO to determine stub behavior
    */
  def sendRegistrationConfirmation(
      emails: List[String],
      subscriptionId: String,
      idNumberOpt: Option[String]
  ): Future[Unit] = {

    idNumberOpt match {
      case Some(idNumber) =>
        val firstTwo   = idNumber.take(2).toUpperCase
        val shouldFail = firstTwo == "44" || firstTwo == "AA"
        if (shouldFail) {
          logger.warn("[EmailService] Failed to send registration confirmation")
        } else {
          logger.info("[EmailService] Successfully sent registration confirmation")
        }

      case None =>
        logger.info("[EmailService] Successfully sent registration confirmation (no ID provided)")
    }

    Future.successful(())
  }

}
