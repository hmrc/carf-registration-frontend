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

package controllers.organisation

import base.SpecBase
import controllers.routes
import models.{BusinessDetails, IsThisYourBusinessPageDetails}
import models.responses.AddressRegistrationResponse
import pages.IsThisYourBusinessPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.UnableToChangeBusinessView

class UnableToChangeBusinessControllerSpec extends SpecBase {

  "UnableToChangeBusiness Controller" - {
    "must return OK and the correct view for a GET when IsThisYourBusinessPage has details saved in user answers" in {
      val testUserAnswers = emptyUserAnswers.withPage(
        IsThisYourBusinessPage,
        IsThisYourBusinessPageDetails(businessDetails = testBusinessDetails, pageAnswer = None)
      )

      val application = applicationBuilder(userAnswers = Some(testUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.organisation.routes.UnableToChangeBusinessController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UnableToChangeBusinessView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testBusinessDetails, testSignOutUrl, testLoginContinueUrl)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return SEE_OTHER and Redirect the user to JourneyRecovery if IsThisYourBusinessPage details are not saved in user answers" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.organisation.routes.UnableToChangeBusinessController.onPageLoad().url)

        val result = route(application, request).value
        
        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
