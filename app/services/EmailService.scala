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
    * @param idNumber
    *   UTR or NINO to determine stub behavior
    */
  
  def sendRegistrationConfirmation(emails: List[String], subscriptionId: String, idNumber: String): Future[Unit] = {
    val firstChar  = idNumber.take(1).toUpperCase
    val shouldFail = firstChar == "9" || firstChar == "Y"

    if (shouldFail) {
      logger.warn("[EmailService] Failed to send registration confirmation")
    } else {
      logger.info("[EmailService] Successfully sent registration confirmation")
    }

    Future.successful(())
  }

}
