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

import connectors.{EmailConnector, EmailSent, EmailStatus}
import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject() (
    emailConnector: EmailConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  /** Sends a registration confirmation email to the provided addresses.
    *
    * @param contacts
    *   List of (name, email) pairs
    * @param subscriptionId
    *   The CARF User ID (subscription ID) to include in the email content
    * @param idNumberOpt
    *   Optional UTR or NINO to determine stub behavior
    */
  def sendRegistrationConfirmation(
      contacts: List[ContactEmailInfo],
      subscriptionId: String,
      idNumberOpt: Option[String]
  )(implicit hc: HeaderCarrier): Future[Unit] =
    applyStubBehavior(subscriptionId, idNumberOpt) match {
      case Some(failure) => failure
      case None          => sendEmails(contacts, subscriptionId)
    }

  private def applyStubBehavior(subscriptionId: String, idNumberOpt: Option[String]): Option[Future[Unit]] = {
    idNumberOpt match {
      case Some(idNumber) =>
        val firstTwo   = idNumber.take(2).toUpperCase
        val shouldFail = firstTwo == "44" || firstTwo == "AA"
        if (shouldFail) {
          logger.warn("[EmailService] Failed to send registration confirmation stub")
          return Some(Future.failed(new Exception("Stubbed email failure")))
        } else {
          logger.info("[EmailService] Successfully sent registration confirmation stub")
        }

      case None =>
        logger.info("[EmailService] Successfully sent registration confirmation (no ID provided) stub")
    }
    None // No failure, continue with actual email sending
  }

  private def sendEmails(
      contacts: List[ContactEmailInfo],
      subscriptionId: String
  )(implicit hc: HeaderCarrier): Future[Unit] = {

    val templateId    = "carf_registration_successful"
    val carfReference = generateCarfReference(subscriptionId)

    if (contacts.isEmpty) {
      logger.warn(s"No contacts to send registration confirmation emails to")
      Future.successful(())
    } else {
      logger.info(s"Sending ${contacts.length} registration confirmation emails")

      val emailRequests = contacts.map { contact =>
        val parameters = Map(
          "name"          -> contact.name,
          "carfReference" -> carfReference
        )

        emailConnector.sendEmail(
          emailAddress = contact.email,
          templateName = templateId,
          templateParams = parameters
        )
      }

      Future.sequence(emailRequests).map { statuses =>
        val successCount = statuses.count(_ == EmailSent)
        val failureCount = statuses.length - successCount

        if (failureCount > 0) {
          logger.warn(s"Failed to send $failureCount out of ${statuses.length} email(s) for CARF")
        }

        logger.info(s"Successfully sent $successCount out of ${statuses.length} registration confirmation email(s)")
      }
    }
  }
  private def generateCarfReference(subscriptionId: String): String                                        = s"XXCAR${subscriptionId.take(10).toUpperCase}"
}
