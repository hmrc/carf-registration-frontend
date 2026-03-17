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

package controllers.actions

import base.SpecBase
import config.FrontendAppConfig
import controllers.routes
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Results.Ok
import play.api.mvc.{BodyParsers, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, internalId}
import uk.gov.hmrc.auth.core.retrieve.~

import scala.concurrent.Future

class CarfIdRetrievalSpec extends SpecBase {

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockAppConfig)
    super.beforeEach()
  }

  val mockAuthConnector: AuthConnector       = mock[AuthConnector]
  val mockAppConfig: FrontendAppConfig       = mock[FrontendAppConfig]
  val defaultBodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]

  val carfKey        = "HMRC-CARF-ORG"
  val state          = "Activated"
  val identifierName = "CARFID"
  val testContent    = "Test"

  val carfEnrolment: Enrolments = Enrolments(
    Set(Enrolment(carfKey, Seq(EnrolmentIdentifier(identifierName, "456")), state))
  )
  val emptyEnrolments           = Enrolments(Set.empty[Enrolment])

  val testCarfIdRetrievalActionExtractor: CarfIdRetrievalActionExtractor =
    new CarfIdRetrievalActionExtractor(mockAuthConnector, mockAppConfig, defaultBodyParser)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok(testContent))
  }

  "CarfIdRetrievalActionExtractor.invokeBlock" - {
    "execute the block and return OK if authorised and carf id can be retrieved from enrolments" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments
          )
        )(any(), any())
      )
        .thenReturn(Future(new ~(Some(testInternalId), carfEnrolment)))

      val result: Future[Result] = testCarfIdRetrievalActionExtractor.invokeBlock(FakeRequest(), testAction)

      status(result)          mustBe OK
      contentAsString(result) mustBe testContent
    }

    "redirect to Index Page (start of registration journey) if carf enrolment is not present" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments
          )
        )(any(), any())
      )
        .thenReturn(Future(new ~(Some(testInternalId), emptyEnrolments)))

      val result: Future[Result] = testCarfIdRetrievalActionExtractor.invokeBlock(FakeRequest(), testAction)

      status(result)           mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IndexController.onPageLoad().url)
    }

    "redirect to unauthorised controller if internal id cannot be retrieved" in {
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments
          )
        )(any(), any())
      )
        .thenReturn(Future(new ~(None, emptyEnrolments)))

      val result: Future[Result] = testCarfIdRetrievalActionExtractor.invokeBlock(FakeRequest(), testAction)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(s"${routes.UnauthorisedController.onPageLoad()}")
    }

    "throw an exception when the auth connector call fails" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments
          )
        )(any(), any())
      )
        .thenReturn(Future.failed(new RuntimeException("bang")))

      val result = intercept[RuntimeException] {
        await(testCarfIdRetrievalActionExtractor.invokeBlock(FakeRequest(), testAction))
      }

      result.getMessage must include("bang")
    }

    "redirect to the login page if no longer authorised or never logged in" in {
      List(
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        when(mockAppConfig.loginUrl).thenReturn("/test")
        when(mockAppConfig.loginContinueUrl).thenReturn("/test2")
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

        val result: Future[Result] = testCarfIdRetrievalActionExtractor.invokeBlock(FakeRequest(), testAction)

        status(result)                 mustBe SEE_OTHER
        redirectLocation(result).value mustBe s"/test?continue=%2Ftest2"
      }
    }
  }

}
