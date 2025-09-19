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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Results.Ok
import play.api.mvc.{BodyParsers, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, credentialRole, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.Future

class IdentifierActionSpec extends SpecBase {

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

  val testIdentifierAction: AuthenticatedIdentifierActionWithRegime =
    new AuthenticatedIdentifierActionWithRegime(mockAuthConnector, mockAppConfig, defaultBodyParser, true)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok(testContent))
  }

  "AuthenticatedIdentifierActionWithRegime.invokeBlock" - {
    "execute the block and return OK if authorised" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments and affinityGroup and credentialRole
          )
        )(any(), any())
      )
        .thenReturn(Future(new ~(new ~(new ~(Some(testInternalId), emptyEnrolments), Some(Organisation)), Some(User))))

      val result: Future[Result] = testIdentifierAction.invokeBlock(FakeRequest(), testAction)

      status(result)          mustBe OK
      contentAsString(result) mustBe testContent
    }
    "return ok with a message if credential role is Assistant" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments and affinityGroup and credentialRole
          )
        )(any(), any())
      )
        .thenReturn(
          Future(new ~(new ~(new ~(Some(testInternalId), emptyEnrolments), Some(Organisation)), Some(Assistant)))
        )

      val result: Future[Result] = testIdentifierAction.invokeBlock(FakeRequest(), testAction)

      status(result)        mustBe OK
      contentAsString(result) must include("User is an assistant so cannot use the service")
    }
    "return ok with a message if affinity group is Agent" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments and affinityGroup and credentialRole
          )
        )(any(), any())
      )
        .thenReturn(Future(new ~(new ~(new ~(Some(testInternalId), emptyEnrolments), Some(Agent)), Some(User))))

      val result: Future[Result] = testIdentifierAction.invokeBlock(FakeRequest(), testAction)

      status(result)        mustBe OK
      contentAsString(result) must include("User is an agent so cannot use the service")
    }
    "return ok with a message if the user is already registered for this service" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments and affinityGroup and credentialRole
          )
        )(any(), any())
      )
        .thenReturn(Future(new ~(new ~(new ~(Some(testInternalId), carfEnrolment), Some(Organisation)), Some(User))))

      val result: Future[Result] = testIdentifierAction.invokeBlock(FakeRequest(), testAction)

      status(result)        mustBe OK
      contentAsString(result) must include("User is already enrolled!")
    }
    "throw an exception if affinity group cannot be retrieved" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments and affinityGroup and credentialRole
          )
        )(any(), any())
      )
        .thenReturn(Future(new ~(new ~(new ~(Some(testInternalId), emptyEnrolments), None), Some(User))))

      val result = intercept[UnauthorizedException] {
        await(testIdentifierAction.invokeBlock(FakeRequest(), testAction))
      }

      result.getMessage must include("Failed to retrieve valid auth data")
    }
    "throw an exception if internal id cannot be retrieved" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments and affinityGroup and credentialRole
          )
        )(any(), any())
      )
        .thenReturn(Future(new ~(new ~(new ~(None, emptyEnrolments), Some(Organisation)), Some(User))))

      val result = intercept[UnauthorizedException] {
        await(testIdentifierAction.invokeBlock(FakeRequest(), testAction))
      }

      result.getMessage must include("Failed to retrieve valid auth data")
    }
    "throw an exception when the auth connector call fails" in {
      when(mockAppConfig.enrolmentKey).thenReturn(carfKey)
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(
            internalId and allEnrolments and affinityGroup and credentialRole
          )
        )(any(), any())
      )
        .thenReturn(Future.failed(new RuntimeException("bang")))

      val result = intercept[RuntimeException] {
        await(testIdentifierAction.invokeBlock(FakeRequest(), testAction))
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

        val result: Future[Result] = testIdentifierAction.invokeBlock(FakeRequest(), testAction)

        status(result)                 mustBe SEE_OTHER
        redirectLocation(result).value mustBe s"/test?continue=%2Ftest2"
      }
    }
  }

}
