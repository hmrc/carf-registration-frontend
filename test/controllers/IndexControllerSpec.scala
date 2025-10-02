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

package controllers

import base.SpecBase
import controllers.actions.{CtUtrRetrievalAction, FakeCtUtrRetrievalAction}
import repositories.SessionRepository
import models.{NormalMode, UserAnswers}
import pages.AutoMatchedUTRPage
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{reset, verify, when}
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}

import java.time.Clock
import scala.concurrent.Future

class IndexControllerSpec extends SpecBase {

  override def beforeEach(): Unit = {
    reset(mockCtUtrRetrievalAction)
    reset(mockSessionRepository)

    when(mockSessionRepository.get(any[String])).thenReturn(Future.successful(None))
    when(mockSessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

    super.beforeEach()
  }

  "Index Controller" - {

    "must handle an individual user correctly" in new Setup(Individual) {

      when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction())

      val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

      val result: Future[Result] = route(application, request).value

      status(result)     mustEqual OK
      contentAsString(result) must include("Take user to: Individual – What Are You Registering As? page (CARF-120)")

    }

    "must handle an organisation user with utr correctly" in new Setup(Organisation) {

      when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction(utr = Some(testUtr)))

      val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

      val result: Future[Result] = route(application, request).value

      status(result)                 mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual controllers.routes.IsThisYourBusinessController
        .onPageLoad(NormalMode)
        .url

      verify(mockSessionRepository).set(argThat[UserAnswers](ua => ua.get(AutoMatchedUTRPage).contains(testUtr)))

    }

    "must handle an organisation user without utr correctly" in new Setup(Organisation) {

      when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction(utr = None))

      val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

      val result: Future[Result] = route(application, request).value

      status(result)        mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(
        controllers.routes.OrganisationRegistrationTypeController.onPageLoad(NormalMode).url
      )
    }

  }
  class Setup(affinityGroup: AffinityGroup) {
    val application: Application = applicationBuilder(affinityGroup = affinityGroup)
      .overrides(
        bind[CtUtrRetrievalAction].toInstance(mockCtUtrRetrievalAction),
        bind[Clock].toInstance(fixedClock)
      )
      .build()
  }
}
