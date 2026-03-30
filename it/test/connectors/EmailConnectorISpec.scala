/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import itutil.{ApplicationWithWiremock, WireMockConstants}
import config.FrontendAppConfig
import models.SendEmailRequest
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.json.Json
import com.github.tomakehurst.wiremock.client.WireMock._
import scala.concurrent.ExecutionContext

class EmailConnectorISpec extends AnyWordSpec with Matchers with ApplicationWithWiremock {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val request: Request[_]  = FakeRequest()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(
      extraConfig ++ Map(
        "microservice.services.email.protocol" -> "http",
        "microservice.services.email.host"     -> wireMock.stubHost,
        "microservice.services.email.port"     -> wireMock.port(),
        "microservice.services.email.url"      -> s"${wireMock.url}/hmrc/email",
        "microservice.services.email.force"    -> true
      )
    )
    .build()

  lazy val connector: EmailConnector = app.injector.instanceOf[EmailConnector]

  "EmailConnector" when {

    "calling .sendEmail()" must {

      val testRecipient  = "testRecipient@example.com"
      val testTemplateId = "test-template"
      val templateParams = Map("carfId" -> "AB123456")

      val emailRequest = SendEmailRequest(
        to = List(testRecipient),
        templateId = testTemplateId,
        parameters = templateParams,
        force = true
      )

      val emailRequestJson = Json.toJson(emailRequest)

      def stubEmailRequest(status: Int): Unit =
        stubFor(
          post(urlPathEqualTo("/hmrc/email"))
            .withRequestBody(equalToJson(emailRequestJson.toString))
            .willReturn(aResponse().withStatus(status))
        )

      "return EmailSent when backend returns 202 ACCEPTED" in {
        stubEmailRequest(ACCEPTED)
        val result = await(connector.sendEmail(testRecipient, testTemplateId, templateParams))
        result shouldBe EmailSent
      }

      "return EmailNotSent when backend returns 404 NOT_FOUND" in {
        stubEmailRequest(NOT_FOUND)
        val result = await(connector.sendEmail(testRecipient, testTemplateId, templateParams))
        result shouldBe EmailNotSent
      }

      "return EmailNotSent when backend returns 500 INTERNAL_SERVER_ERROR" in {
        stubEmailRequest(INTERNAL_SERVER_ERROR)
        val result = await(connector.sendEmail(testRecipient, testTemplateId, templateParams))
        result shouldBe EmailNotSent
      }

      "return EmailNotSent when the backend cannot be reached" in {
        val badApp = new GuiceApplicationBuilder()
          .configure(
            extraConfig ++ Map(
              "microservice.services.email.host"  -> "localhost",
              "microservice.services.email.port"  -> 9999,
              "microservice.services.email.url"   -> "http://localhost:9999",
              "microservice.services.email.force" -> true
            )
          )
          .build()

        val badConnector = badApp.injector.instanceOf[EmailConnector]

        val result = await(badConnector.sendEmail(testRecipient, testTemplateId, templateParams))
        result shouldBe EmailNotSent

        badApp.stop()
      }
    }
  }
}
