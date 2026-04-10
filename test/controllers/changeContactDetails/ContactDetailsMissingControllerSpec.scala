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

package controllers.changeContactDetails

import base.SpecBase
import play.api.Application
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.ContactDetailsMissingdView

import scala.concurrent.Future

class ContactDetailsMissingControllerSpec extends SpecBase {

  "Contact Details Missing Controller" - {
    "onPageLoad" - {
      "must return ok with the view with individual continueUrl" in {

        val userAnswers = emptyUserAnswers.copy(changeIsIndividualRegType = Some(true))

        val application: Application =
          applicationBuilder(userAnswers = Some(userAnswers)).build()

        val expectedContinueUrl =
          controllers.changeContactDetails.routes.ChangeIndividualEmailController.onPageLoad().url

        lazy val pageRoute: String =
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url

        val request                = FakeRequest(GET, pageRoute)
        val view                   = application.injector.instanceOf[ContactDetailsMissingdView]
        val result: Future[Result] = route(application, request).value

        status(result)          mustBe OK
        contentAsString(result) mustBe view(expectedContinueUrl)(
          request,
          messages(application)
        ).toString
      }

      "must return ok with the view with organisation continueUrl" in {

        val userAnswers = emptyUserAnswers.copy(changeIsIndividualRegType = Some(false))

        val application: Application =
          applicationBuilder(userAnswers = Some(userAnswers)).build()

        val expectedContinueUrl = controllers.routes.PlaceholderController
          .onPageLoad("Redirect to /change-contact/organisation/email CARF-186")
          .url

        lazy val pageRoute: String =
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url

        val request                = FakeRequest(GET, pageRoute)
        val view                   = application.injector.instanceOf[ContactDetailsMissingdView]
        val result: Future[Result] = route(application, request).value

        status(result)          mustBe OK
        contentAsString(result) mustBe view(expectedContinueUrl)(
          request,
          messages(application)
        ).toString
      }

      "must redirect to journey recovery when changeIsIndividualRegType is missing from user answers" in {

        val application: Application =
          applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        lazy val pageRoute: String =
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url

        val request                = FakeRequest(GET, pageRoute)
        val view                   = application.injector.instanceOf[ContactDetailsMissingdView]
        val result: Future[Result] = route(application, request).value

        status(result)              mustEqual SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

      }
    }
  }
}
