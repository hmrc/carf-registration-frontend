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

import scala.concurrent.Future

sealed trait EmailResult

object EmailResult {
  case object Sent extends EmailResult
  final case class Failed(reason: String) extends EmailResult
}

trait EmailService {

  /** Sends a registration confirmation email to the provided addresses.
    *
    * @param emails
    *   List of email addresses to notify (must include the mandatory primary address; optional second address may be
    *   included)
    * @param subscriptionId
    *   The CARF User ID (subscription ID) to include in the email content
    * @return
    *   Future of EmailResult indicating success or failure
    */
  def sendRegistrationConfirmation(emails: List[String], subscriptionId: String): Future[EmailResult]
}
