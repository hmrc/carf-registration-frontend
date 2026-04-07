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
import connectors.{EmailConnector, EmailNotSent, EmailSent}
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

    "sendEmails" - {

      "must successfully send email for a single contact with CARF reference" in {
        val contact        = ContactEmailInfo("John Doe", "john@example.com")
        val subscriptionId = "sub123"
        val contacts       = List(contact)

        val expectedParams = Map(
          "name"          -> contact.name,
          "carfReference" -> subscriptionId
        )

        when(
          mockEmailConnector.sendEmail(
            meq(contact.email),
            meq("carf_registration_successful"),
            meq(expectedParams)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        ).thenReturn(Future.successful(EmailSent))

        val result =
          service.sendEmails(contacts, Some(subscriptionId), haveEmailsSentAlready = false)

        whenReady(result) { _ =>
          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact.email),
            meq("carf_registration_successful"),
            meq(expectedParams)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must successfully send email for a single contact without CARF reference" in {
        val contact  = ContactEmailInfo("John Doe", "john@example.com")
        val contacts = List(contact)

        val expectedParams = Map(
          "name" -> contact.name
        )

        when(
          mockEmailConnector.sendEmail(
            meq(contact.email),
            meq("carf_registration_successful"),
            meq(expectedParams)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        ).thenReturn(Future.successful(EmailSent))

        val result =
          service.sendEmails(contacts, None, haveEmailsSentAlready = false)

        whenReady(result) { _ =>
          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact.email),
            meq("carf_registration_successful"),
            meq(expectedParams)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must send emails for multiple contacts with CARF reference" in {
        val contact1       = ContactEmailInfo("John Doe", "john@example.com")
        val contact2       = ContactEmailInfo("Jane Smith", "jane@example.com")
        val subscriptionId = "sub123"
        val contacts       = List(contact1, contact2)

        val params1 = Map(
          "name"          -> contact1.name,
          "carfReference" -> subscriptionId
        )
        val params2 = Map(
          "name"          -> contact2.name,
          "carfReference" -> subscriptionId
        )

        when(
          mockEmailConnector.sendEmail(meq(contact1.email), any(), meq(params1))(
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        ).thenReturn(Future.successful(EmailSent))

        when(
          mockEmailConnector.sendEmail(meq(contact2.email), any(), meq(params2))(
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        ).thenReturn(Future.successful(EmailSent))

        val result =
          service.sendEmails(contacts, Some(subscriptionId), haveEmailsSentAlready = false)

        whenReady(result) { _ =>
          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact1.email),
            any(),
            meq(params1)
          )(any[HeaderCarrier](), any[ExecutionContext]())

          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact2.email),
            any(),
            meq(params2)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must send emails for multiple contacts without CARF reference" in {
        val contact1 = ContactEmailInfo("John Doe", "john@example.com")
        val contact2 = ContactEmailInfo("Jane Smith", "jane@example.com")
        val contacts = List(contact1, contact2)

        val params1 = Map("name" -> contact1.name)
        val params2 = Map("name" -> contact2.name)

        when(
          mockEmailConnector.sendEmail(meq(contact1.email), any(), meq(params1))(
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        ).thenReturn(Future.successful(EmailSent))

        when(
          mockEmailConnector.sendEmail(meq(contact2.email), any(), meq(params2))(
            any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        ).thenReturn(Future.successful(EmailSent))

        val result =
          service.sendEmails(contacts, None, haveEmailsSentAlready = false)

        whenReady(result) { _ =>
          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact1.email),
            any(),
            meq(params1)
          )(any[HeaderCarrier](), any[ExecutionContext]())

          verify(mockEmailConnector, times(1)).sendEmail(
            meq(contact2.email),
            any(),
            meq(params2)
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must handle empty contact list without sending emails" in {
        val result = service.sendEmails(Nil, Some("sub123"), haveEmailsSentAlready = false)

        whenReady(result) { _ =>
          verify(mockEmailConnector, never()).sendEmail(
            any[String](),
            any[String](),
            any[Map[String, String]]()
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must skip sending emails if haveEmailsSentAlready is true" in {
        val contact  = ContactEmailInfo("John Doe", "john@example.com")
        val contacts = List(contact)

        val result = service.sendEmails(contacts, Some("sub123"), haveEmailsSentAlready = true)

        whenReady(result) { _ =>
          verify(mockEmailConnector, never()).sendEmail(
            any[String](),
            any[String](),
            any[Map[String, String]]()
          )(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must handle email connector failures gracefully" in {
        val contact1       = ContactEmailInfo("John Doe", "john@example.com")
        val contact2       = ContactEmailInfo("Jane Smith", "jane@example.com")
        val subscriptionId = "sub123"
        val contacts       = List(contact1, contact2)

        when(
          mockEmailConnector.sendEmail(meq(contact1.email), any(), any())(any(), any())
        ).thenReturn(Future.successful(EmailSent))

        when(
          mockEmailConnector.sendEmail(meq(contact2.email), any(), any())(any(), any())
        ).thenReturn(Future.successful(EmailNotSent))

        val result =
          service.sendEmails(contacts, Some(subscriptionId), haveEmailsSentAlready = false)

        whenReady(result) { _ =>
          verify(mockEmailConnector, times(2)).sendEmail(any(), any(), any())(any(), any())
        }
      }
    }
  }
}
