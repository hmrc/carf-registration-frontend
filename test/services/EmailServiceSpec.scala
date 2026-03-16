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
import connectors.{EmailConnector, EmailSent, EmailStatus}
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class EmailServiceSpec extends SpecBase with MockitoSugar {

  private val mockEmailConnector = mock[EmailConnector]
  private val service            = new EmailService(mockEmailConnector)

  override implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    reset(mockEmailConnector)
    super.beforeEach()
  }

  "EmailService" - {

    "sendRegistrationConfirmation" - {

      "must successfully send email for a single contact with ID" in {
        val contact        = ContactEmailInfo("John Doe", "john@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("1234567890")
        val contacts       = List(contact)

        val expectedParams = Map(
          "name"          -> contact.name,
          "carfReference" -> "XXCARSUB123"
        )

        when(
          mockEmailConnector.sendEmail(
            meq(contact.email),
            meq("carf_registration_successful"),
            meq(expectedParams)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        ).thenReturn(Future.successful(EmailSent))

        val result = service.sendRegistrationConfirmation(contacts, subscriptionId, idNumberOpt)

        whenReady(result) { _ =>
          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact.email),
            meq("carf_registration_successful"),
            meq(expectedParams)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must send emails for multiple contacts" in {
        val contact1       = ContactEmailInfo("John Doe", "john@example.com")
        val contact2       = ContactEmailInfo("Jane Smith", "jane@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("1234567890")
        val contacts       = List(contact1, contact2)

        val params1 = Map(
          "name"          -> contact1.name,
          "carfReference" -> "XXCARSUB123"
        )
        val params2 = Map(
          "name"          -> contact2.name,
          "carfReference" -> "XXCARSUB123"
        )

        when(
          mockEmailConnector.sendEmail(meq(contact1.email), meq("carf_registration_successful"), meq(params1))(
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        )
          .thenReturn(Future.successful(EmailSent))

        when(
          mockEmailConnector.sendEmail(meq(contact2.email), meq("carf_registration_successful"), meq(params2))(
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        )
          .thenReturn(Future.successful(EmailSent))

        val result = service.sendRegistrationConfirmation(contacts, subscriptionId, idNumberOpt)

        whenReady(result) { _ =>
          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact1.email),
            meq("carf_registration_successful"),
            meq(params1)
          )(any[HeaderCarrier](), any[ExecutionContext]())
          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact2.email),
            meq("carf_registration_successful"),
            meq(params2)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must fail when idNumber starts with 44 (stub failure)" in {
        val contact        = ContactEmailInfo("John Doe", "john@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("4412345678")
        val contacts       = List(contact)

        val result = service.sendRegistrationConfirmation(contacts, subscriptionId, idNumberOpt)

        whenReady(result.failed) { ex =>
          ex          mustBe a[Exception]
          ex.getMessage must include("Stubbed email failure")
          verify(mockEmailConnector, never()).sendEmail(any[String](), any[String](), any[Map[String, String]]())(
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        }
      }

      "must fail when idNumber starts with AA (stub failure)" in {
        val contact        = ContactEmailInfo("John Doe", "john@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("AA12345678")
        val contacts       = List(contact)

        val result = service.sendRegistrationConfirmation(contacts, subscriptionId, idNumberOpt)

        whenReady(result.failed) { ex =>
          ex          mustBe a[Exception]
          ex.getMessage must include("Stubbed email failure")
          verify(mockEmailConnector, never()).sendEmail(any[String](), any[String](), any[Map[String, String]]())(
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        }
      }

      "must send email when user is withoutId" in {
        val contact        = ContactEmailInfo("John Doe", "john@example.com")
        val subscriptionId = "sub123"
        val idNumberOpt    = None
        val contacts       = List(contact)

        val expectedParams = Map(
          "name"          -> contact.name,
          "carfReference" -> "XXCARSUB123"
        )

        when(
          mockEmailConnector.sendEmail(
            meq(contact.email),
            meq("carf_registration_successful"),
            meq(expectedParams)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        ).thenReturn(Future.successful(EmailSent))

        val result = service.sendRegistrationConfirmation(contacts, subscriptionId, idNumberOpt)

        whenReady(result) { _ =>
          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact.email),
            meq("carf_registration_successful"),
            meq(expectedParams)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must handle empty contact list without sending emails" in {
        val contacts       = Nil
        val subscriptionId = "sub123"
        val idNumberOpt    = Some("1234567890")

        val result = service.sendRegistrationConfirmation(contacts, subscriptionId, idNumberOpt)

        whenReady(result) { _ =>
          verify(mockEmailConnector, never()).sendEmail(
            any[String](),
            any[String](),
            any[Map[String, String]]()
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

    }
  }

}
