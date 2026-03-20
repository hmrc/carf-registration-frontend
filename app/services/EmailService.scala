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

import connectors.{EmailConnector, EmailSent}
import javax.inject.{Inject, Singleton}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject() (
    emailConnector: EmailConnector
)(implicit ec: ExecutionContext)
    extends Logging {

  // Add extra param in here. What's a simple way we can us it here to reduce code duplication in the controller?
  private def sendEmails(
      contacts: List[ContactEmailInfo],
      subscriptionId: String
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    // TODO move this to Constants file
    val templateId = "carf_registration_successful"

    if (contacts.isEmpty) {
      logger.warn("No contacts to send registration confirmation emails to")
      Future.successful((): Unit)
    } else {
      Future
        .traverse(contacts) { contact =>
          val parameters = Map(
            "name"          -> contact.name,
            "carfReference" -> subscriptionId
          )
          emailConnector.sendEmail(contact.email, templateId, parameters)
        }
        .map { statuses =>
          val successCount = statuses.count(_ == EmailSent)
          val failureCount = statuses.length - successCount

          if (failureCount > 0) {
            logger.warn(s"Failed to send $failureCount out of ${statuses.length} registration confirmation email(s)")
          }

          logger.info(s"Successfully sent $successCount out of ${statuses.length} registration confirmation email(s)")
        }
    }
  }
}
