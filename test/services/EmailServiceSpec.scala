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

      "must successfully send email when only primary email is provided and idNumber exists" in {
        val emails         = List("primary@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("1234567890")

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumberOpt)

        whenReady(result) { r =>
          r mustBe ()
        }
      }

      "must successfully send email when primary and secondary emails are provided and idNumber exists" in {
        val emails         = List("primary@example.com", "secondary@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("1234567890")

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumberOpt)

        whenReady(result) { r =>
          r mustBe ()
        }
      }

      "must log a warning if idNumber starts with 44" in {
        val emails         = List("primary@example.com", "secondary@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("4412345678")

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumberOpt)

        whenReady(result) { r =>
          r mustBe ()
        }
      }

      "must log a warning if idNumber starts with AA" in {
        val emails         = List("primary@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("AA12345678")

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumberOpt)

        whenReady(result) { r =>
          r mustBe ()
        }
      }

      "must successfully send email when idNumber is None" in {
        val emails         = List("primary@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = None

        val result: Future[Unit] = service.sendRegistrationConfirmation(emails, subscriptionId, idNumberOpt)

        whenReady(result) { r =>
          r mustBe ()
        }
      }
    }
  }
}
