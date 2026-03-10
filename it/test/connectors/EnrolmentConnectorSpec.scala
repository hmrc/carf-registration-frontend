package connectors

import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import com.github.tomakehurst.wiremock.client.WireMock.*
import itutil.ApplicationWithWiremock
import models.error.ApiError
import models.requests.{EnrolmentRequest, Identifier, Verifier}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}

class EnrolmentConnectorSpec extends ApplicationWithWiremock
  with Matchers
  with ScalaFutures
  with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: EnrolmentConnector = app.injector.instanceOf[EnrolmentConnector]

  val enrolmentRequestValidAsJson: JsValue = Json.parse(
    s"""
       |{
       |  "identifiers": [ { "key": "CARFID", "value": "AA000003D" } ],
       |  "verifiers": [
       |          {
       |              "key": "PostCode",
       |              "value": "N15 2FY"
       |          },
       |          {
       |              "key": "IsAbroad",
       |              "value": "N"
       |          }
       |   ]
       |}
       |""".stripMargin
  )

  val enrolmentRequestValid = EnrolmentRequest(
    Seq(Identifier("CARFID", "AA000003D")),
    Seq(
      Verifier("PostCode", "N15 2FY"),
      Verifier("IsAbroad", "N"),
    )
  )

  "createEnrolment" should {
    "successfully receive NO Content response" in {

      stubFor(
        put(urlPathMatching("/dac6/dprs0101/v1"))
          .withRequestBody(equalToJson(enrolmentRequestValidAsJson.toString))
          .willReturn(
            aResponse()
              .withStatus(NO_CONTENT)
          )
      )

      val result = connector.createEnrolment(enrolmentRequestValid).value.futureValue

      assert(result.isRight)

    }

    "return Left when a Bad Request response status is received" in {
      stubFor(
        put(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(
            aResponse()
              .withStatus(BAD_REQUEST)
              .withBody("Provided service name is not in services-to-activate or No group ID in active auth session")
          )
      )

      val result = connector.createEnrolment(enrolmentRequestValid).value.futureValue

      result shouldBe Left(ApiError.BadRequestError)

    }
    
    "return Left when a Internal Server Error response status is received" in {
      stubFor(
        put(urlPathMatching("/dac6/dprs0101/v1"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("Unexpected response")
          )
      )

      val result = connector.createEnrolment(enrolmentRequestValid).value.futureValue

      result shouldBe Left(ApiError.InternalServerError)

    }
  }
}
