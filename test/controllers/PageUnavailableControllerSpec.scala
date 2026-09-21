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

package controllers

import base.SpecBase
import config.FrontendAppConfig
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.problem.PageUnavailableView

class PageUnavailableControllerSpec extends SpecBase {

  val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  "PageUnavailable Controller" - {

    "must return OK and the correct view for a GET" in {

      when(mockAppConfig.carfManagementFrontendHomePageUrl).thenReturn("foo")
      when(mockAppConfig.feedbackUrl(any())).thenReturn("foo")

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[FrontendAppConfig].toInstance(mockAppConfig))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.PageUnavailableController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PageUnavailableView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view("foo")(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
