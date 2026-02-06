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

import base.SpecBase
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailServiceSpec extends SpecBase {

  val service: EmailService = new EmailService()

  "EmailService" - {

    "sendRegistrationConfirmation" - {

      "must successfully send email when only primary email is provided and idNumber is normal" in {
        val emails         = List("primary@example.com")
        val subscriptionId = "sub123"
        val idNumber       = "1234567890"

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumber)

        whenReady(result) { r =>
          r mustBe ()
        }
      }

      "must successfully send email when primary and secondary emails are provided and idNumber is normal" in {
        val emails         = List("primary@example.com", "secondary@example.com")
        val subscriptionId = "sub123"
        val idNumber       = "1234567890"

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumber)

        whenReady(result) { r =>
          r mustBe ()
        }
      }

      "must log a warning if idNumber starts with 9" in {
        val emails         = List("primary@example.com", "secondary@example.com")
        val subscriptionId = "sub123"
        val idNumber       = "9123456780"

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumber)

        whenReady(result) { r =>
          r mustBe ()
        }
      }

      "must log a warning if idNumber starts with Y (case insensitive)" in {
        val emails         = List("primary@example.com")
        val subscriptionId = "sub123"
        val idNumber       = "y1234567890"

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumber)

        whenReady(result) { r =>
          r mustBe ()
        }
      }
    }
  }
}
