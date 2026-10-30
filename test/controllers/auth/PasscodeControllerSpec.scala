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

package controllers.auth

import base.SpecBase
import models.NormalMode
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{times, verify, when}
import pages.PasscodePage
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import scala.concurrent.Future

class PasscodeControllerSpec extends SpecBase {

  "processPasscode" - {
    "must create UserAnswers and set PasscodePage to value passed through and redirect to IndexController" in {

      when(mockSessionRepository.set(any)) thenReturn Future.successful(true)

      val application =
        applicationBuilder(None)
          .build()

      running(application) {

        val request = FakeRequest(GET, routes.PasscodeController.processPasscode("foo").url)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.IndexController.onPageLoad(NormalMode).url
        verify(mockSessionRepository, times(1)).set(argThat(ua => ua.get(PasscodePage).contains("foo")))
      }
    }
  }

  "processPasscodeChangeDetails" - {
    "must create UserAnswers and set PasscodePage to value passed through and redirect to ChangeContactDetailsIndexController" in {

      when(mockSessionRepository.set(any)) thenReturn Future.successful(true)

      val application =
        applicationBuilder(None)
          .build()

      running(application) {

        val request = FakeRequest(GET, routes.PasscodeController.processPasscodeChangeDetails("foo").url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value        mustEqual controllers.changeContactDetails.routes.ChangeContactDetailsIndexController.onPageLoad().url
        verify(mockSessionRepository, times(1)).set(argThat(ua => ua.get(PasscodePage).contains("foo")))
      }
    }
  }
}
