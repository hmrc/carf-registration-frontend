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
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.internalId

import scala.concurrent.Future

class PasscodeActionSpec extends SpecBase {

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockAppConfig)
    super.beforeEach()
  }

  val mockAuthConnector: AuthConnector       = mock[AuthConnector]
  val mockAppConfig: FrontendAppConfig       = mock[FrontendAppConfig]
  val defaultBodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]

  val testContent  = "Test"
  val testPasscode = "Test123"

  val testPasscodeAction: PasscodeActionExtractor = new PasscodeActionExtractor(
    mockAuthConnector,
    mockAppConfig,
    defaultBodyParser,
    testPasscode
  )

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok(testContent))
  }

  "PasscodeActionExtractor.invokeBlock" - {

    "execute the block and return OK when id can be retrieved" in {
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(internalId)
        )(any(), any())
      ).thenReturn(Future(Some(testInternalId)))

      val result: Future[Result] = testPasscodeAction.invokeBlock(FakeRequest(), testAction)

      status(result)          mustBe OK
      contentAsString(result) mustBe testContent
    }

    "redirect to Placeholder when id cannot be retrieved" in {
      when(
        mockAuthConnector.authorise(
          ArgumentMatchers.eq(EmptyPredicate),
          ArgumentMatchers.eq(internalId)
        )(any(), any())
      ).thenReturn(Future(None))

      val result: Future[Result] = testPasscodeAction.invokeBlock(FakeRequest(), testAction)

      status(result)           mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.PlaceholderController.onPageLoad("Could not retrieve id!").url
      )
    }

    "redirect to the login page with the passcode page as the continue redirect as if no longer authorised or never logged in" in {
      List(
        BearerTokenExpired(),
        MissingBearerToken(),
        InvalidBearerToken(),
        SessionRecordNotFound()
      ).foreach { exception =>
        when(mockAppConfig.loginUrl).thenReturn("/test")
        when(mockAppConfig.loginContinuePasscodeUrl).thenReturn("/passcode")
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

        val result: Future[Result] = testPasscodeAction.invokeBlock(FakeRequest(), testAction)

        status(result)                 mustBe SEE_OTHER
        redirectLocation(result).value mustBe s"/test?continue=%2Fpasscode%2FTest123"
      }
    }

  }
}
