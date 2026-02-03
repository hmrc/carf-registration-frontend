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
import play.api.{Configuration, Logging}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StubEmailService @Inject() (
    config: Configuration
)(implicit ec: ExecutionContext)
    extends EmailService
    with Logging {

  private val stubSuccess: Boolean =
    config.getOptional[Boolean]("email.stub.success").getOrElse(true)

  override def sendRegistrationConfirmation(emails: List[String], subscriptionId: String): Future[EmailResult] = {
    logger.info(
      s"[StubEmailService] sendRegistrationConfirmation to=${emails.mkString(", ")} " +
        s"subscriptionId=$subscriptionId (stubSuccess=$stubSuccess)"
    )

    if (stubSuccess) {
      Future.successful(EmailResult.Sent)
    } else {
      Future.successful(EmailResult.Failed("Stubbed email failure"))
    }
  }
}
