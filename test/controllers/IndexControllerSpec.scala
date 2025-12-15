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
import models.{NormalMode, UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{times, verify, when}
import pages.IndexPage
import pages.individual.HaveNiNumberPage
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}

import java.time.Clock
import scala.concurrent.Future

class IndexControllerSpec extends SpecBase {

  val testExistingUserAnswers: UserAnswers        =
    UserAnswers(id = "test-id", lastUpdated = clock.instant()).set(HaveNiNumberPage, true).success.value
  val testExistingUserAnswersWithUtr: UserAnswers = testExistingUserAnswers.set(IndexPage, testUtr).success.value

  "Index Controller" - {
    "individual user must" - {
      "must be redirected successfully" in new Setup(Individual) {
        when(mockSessionRepository.set(any)).thenReturn(Future.successful(true))
        when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction())

        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IndividualRegistrationTypeController.onPageLoad(NormalMode).url
        )
        verify(mockSessionRepository, times(1)).set(any())
      }

      "persist user answers if they exist in the request" in new Setup(
        Individual,
        None,
        Some(testExistingUserAnswers)
      ) {
        when(mockSessionRepository.set(testExistingUserAnswers)).thenReturn(Future.successful(true))
        when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction())

        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.routes.IndividualRegistrationTypeController.onPageLoad(NormalMode).url
        )
        verify(mockSessionRepository, times(1)).set(testExistingUserAnswers)
      }
    }

    "organisation user with ct-utr from enrolments must" - {
      "be handled correctly" in new Setup(
        Organisation,
        Some(testUtr.uniqueTaxPayerReference)
      ) {
        when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction(utr = Some(testUtr)))
        when(mockSessionRepository.set(any)).thenReturn(Future.successful(true))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.IsThisYourBusinessController
          .onPageLoad(NormalMode)
          .url
        verify(mockSessionRepository).set(argThat(ua => ua.get(IndexPage).contains(testUtr)))
      }

      "persist user answers if they exist in the request" in new Setup(
        Organisation,
        Some(testUtr.uniqueTaxPayerReference),
        Some(testExistingUserAnswers)
      ) {
        when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction(utr = Some(testUtr)))
        when(mockSessionRepository.set(testExistingUserAnswersWithUtr)).thenReturn(Future.successful(true))

        val request                = FakeRequest(GET, routes.IndexController.onPageLoad().url)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.IsThisYourBusinessController
          .onPageLoad(NormalMode)
          .url
        verify(mockSessionRepository, times(1)).set(testExistingUserAnswersWithUtr)
      }
    }

    "organisation user without ct-utr from enrolments must" - {
      "be handled correctly when NO utr from enrolments" in new Setup(Organisation) {
        when(mockSessionRepository.set(any)).thenReturn(Future.successful(true))
        when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction(utr = None))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.organisation.routes.OrganisationRegistrationTypeController.onPageLoad(NormalMode).url
        )
        verify(mockSessionRepository, times(1)).set(any())
      }

      "persist user answers if they exist in the request" in new Setup(
        Organisation,
        None,
        Some(testExistingUserAnswers)
      ) {
        when(mockSessionRepository.set(testExistingUserAnswers)).thenReturn(Future.successful(true))
        when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction(utr = None))

        val request = FakeRequest(GET, routes.IndexController.onPageLoad().url)

        val result: Future[Result] = route(application, request).value

        status(result)        mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.organisation.routes.OrganisationRegistrationTypeController.onPageLoad(NormalMode).url
        )
        verify(mockSessionRepository, times(1)).set(testExistingUserAnswers)
      }
    }
  }

  class Setup(
      affinityGroup: AffinityGroup,
      requestUtr: Option[String] = None,
      userAnswers: Option[UserAnswers] = None
  ) {
    val application: Application =
      applicationBuilder(affinityGroup = affinityGroup, requestUtr = requestUtr, userAnswers = userAnswers)
        .overrides(
          bind[CtUtrRetrievalAction].toInstance(mockCtUtrRetrievalAction),
          bind[Clock].toInstance(clock)
        )
        .build()
  }
}
