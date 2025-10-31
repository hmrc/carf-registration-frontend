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

package controllers.orgWithoutId

import base.SpecBase
import base.TestConstants.{businessNameWithInvalidChars, invalidBusinessNameExceeds105Chars, validBusinessName105Chars}
import controllers.routes
import forms.orgWithoutId.OrgWithoutIdBusinessNameFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalactic.Prettifier.default
import org.scalatestplus.mockito.MockitoSugar
import pages.orgWithoutId.OrgWithoutIdBusinessNamePage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import views.html.orgWithoutId.OrgWithoutIdBusinessNameView

import scala.concurrent.Future

class OrgWithoutIdBusinessNameControllerSpec extends SpecBase with MockitoSugar {
  def onwardRoute                                = Call("GET", "/foo")
  val formProvider                               = new OrgWithoutIdBusinessNameFormProvider()
  val form: Form[String]                         = formProvider()
  lazy val orgWithoutIdBusinessNameRoute: String =
    controllers.orgWithoutId.routes.OrgWithoutIdBusinessNameController.onPageLoad(NormalMode).url

  "OrgWithoutIdBusinessName Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, orgWithoutIdBusinessNameRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[OrgWithoutIdBusinessNameView]
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(OrgWithoutIdBusinessNamePage, "valid answer organisation")
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))
      running(application) {
        val request = FakeRequest(GET, orgWithoutIdBusinessNameRoute)
        val result  = route(application, request).value
        val form    = new OrgWithoutIdBusinessNameFormProvider().apply()
        val view    = application.injector.instanceOf[OrgWithoutIdBusinessNameView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill("valid answer organisation"),
          NormalMode
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val userAnswersValidBusinessName = UserAnswers(userAnswersId)
        .set(OrgWithoutIdBusinessNamePage, "valid answer organisation")
        .success
        .value
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val application                  =
        applicationBuilder(userAnswers = Some(userAnswersValidBusinessName), affinityGroup = Organisation)
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()
      running(application) {
        val request =
          FakeRequest(POST, orgWithoutIdBusinessNameRoute)
            .withFormUrlEncodedBody(("value", validBusinessName105Chars))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(GET, orgWithoutIdBusinessNameRoute)
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request =
          FakeRequest(POST, orgWithoutIdBusinessNameRoute)
            .withFormUrlEncodedBody(("value", "answer"))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return Bad Request & Empty-Business-Name error when BusinessName field is empty" in {
      val badOrgWithoutIdBusinessName = ""
      val application                 = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request   = FakeRequest(POST, orgWithoutIdBusinessNameRoute).withFormUrlEncodedBody(
          ("value", badOrgWithoutIdBusinessName)
        )
        val boundForm = form.bind(Map("value" -> badOrgWithoutIdBusinessName))
        val view      = application.injector.instanceOf[OrgWithoutIdBusinessNameView]
        val result    = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
        contentAsString(result)      must include(messages(application)("businessName.error.required"))
      }
    }
    "must return Bad Request & maxLength Error error when OrgWithoutIdBusinessName is longer than 105 chars]" in {
      val badOrgWithoutIdBusinessName = invalidBusinessNameExceeds105Chars
      val application                 = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request   = FakeRequest(POST, orgWithoutIdBusinessNameRoute).withFormUrlEncodedBody(
          ("value", badOrgWithoutIdBusinessName)
        )
        val boundForm = form.bind(Map("value" -> badOrgWithoutIdBusinessName))
        val view      = application.injector.instanceOf[OrgWithoutIdBusinessNameView]
        val result    = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
        contentAsString(result)      must include(messages(application)("businessName.error.maximumLength"))
      }
    }

    "must return Bad Request & invalidFormat error when BusinessName contains an invalid character" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request   = FakeRequest(POST, orgWithoutIdBusinessNameRoute)
          .withFormUrlEncodedBody("value" -> businessNameWithInvalidChars)
        val boundForm = form.bind(Map("value" -> businessNameWithInvalidChars))
        val view      = application.injector.instanceOf[OrgWithoutIdBusinessNameView]
        val result    = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString

        // don't use error message String '(messages(application)("businessName.error.invalidFormat"))' here -
        // the special non-alphanumeric characters are escaped when inserted into html, so can't be directly compared.
        contentAsString(result) must include("Business name must only include letters a to z, numbers 0 to 9")
      }
    }
  }
}
