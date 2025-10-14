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
import forms.BusinessWithoutIdBusinessNameFormProvider
import javax.inject.Inject
import models.{BusinessWithoutIdBusinessName, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalactic.Prettifier.default
import org.scalatestplus.mockito.MockitoSugar
import pages.BusinessWithoutIdBusinessNamePage
import play.api.inject.bind
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import views.html.BusinessWithoutIdBusinessNameView
import config.CarfConstants.validBusinessName105Chars
import config.CarfConstants.invalidBusinessNameExceeds105Chars
import scala.concurrent.Future

class BusinessWithoutIdBusinessNameControllerSpec @Inject() (messagesApi: MessagesApi)
    extends SpecBase
    with MockitoSugar {
  def onwardRoute                             = Call("GET", "/foo")
  val formProvider                            = new BusinessWithoutIdBusinessNameFormProvider()
  val form                                    = formProvider("")
  val invalidCharacterErrorMessage            = messagesApi("businessWithoutIdBusinessName.error.invalidFormat")(Lang("en"))
  lazy val businessWithoutIdBusinessNameRoute =
    routes.BusinessWithoutIdBusinessNameController.onPageLoad(NormalMode).url

  def testInvalidCharacterInBusinessName(badBusinessWithoutIdBusinessName: String): Unit = {
    val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
    running(application) {
      val request = FakeRequest(POST, businessWithoutIdBusinessNameRoute)
        .withFormUrlEncodedBody("value" -> badBusinessWithoutIdBusinessName)

      val boundForm = form.bind(Map("value" -> badBusinessWithoutIdBusinessName))
      val view      = application.injector.instanceOf[BusinessWithoutIdBusinessNameView]
      val result    = route(application, request).value
      status(result)          mustEqual BAD_REQUEST
      contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      contentAsString(result)      must include(invalidCharacterErrorMessage)
    }
  }

  "BusinessWithoutIdBusinessName Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(GET, businessWithoutIdBusinessNameRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[BusinessWithoutIdBusinessNameView]
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val userAnswers = UserAnswers(userAnswersId)
        .set(BusinessWithoutIdBusinessNamePage, BusinessWithoutIdBusinessName("valid answer"))
        .success
        .value
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()
      when(mockSessionRepository.set(userAnswers.copy(data = userAnswers.data))).thenReturn(Future.successful(true))
      running(application) {
        val request = FakeRequest(GET, businessWithoutIdBusinessNameRoute)
        val form    = new BusinessWithoutIdBusinessNameFormProvider().apply(
          businessName = userAnswers.get(BusinessWithoutIdBusinessNamePage).get.toString
        )
        val view    = application.injector.instanceOf[BusinessWithoutIdBusinessNameView]
        val result  = route(application, request).value
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(BusinessWithoutIdBusinessName("valid answer")),
          NormalMode
        )(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      val userAnswersValidBusinessName = UserAnswers(userAnswersId)
        .set(BusinessWithoutIdBusinessNamePage, BusinessWithoutIdBusinessName("valid answer"))
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
          FakeRequest(POST, businessWithoutIdBusinessNameRoute)
            .withFormUrlEncodedBody(("value", validBusinessName105Chars))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(GET, businessWithoutIdBusinessNameRoute)
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request =
          FakeRequest(POST, businessWithoutIdBusinessNameRoute)
            .withFormUrlEncodedBody(("value", "answer"))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    // ========================================== invalid Character tests ==============================================
    "must return Bad Request & Empty-Business-Name error when BusinessName field is empty" in {
      val badBusinessWithoutIdBusinessName = ""
      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request   = FakeRequest(POST, businessWithoutIdBusinessNameRoute).withFormUrlEncodedBody(
          ("value", badBusinessWithoutIdBusinessName)
        )
        val boundForm = form.bind(Map("value" -> badBusinessWithoutIdBusinessName))
        val view      = application.injector.instanceOf[BusinessWithoutIdBusinessNameView]
        val result    = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
        contentAsString(result)      must include("Enter the name of your business")
      }
    }
    "must return Bad Request & invalidFormat error when BusinessWithoutIdBusinessName is longer than 105 chars]" in {
      val badBusinessWithoutIdBusinessName = invalidBusinessNameExceeds105Chars
      val application                      = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request   = FakeRequest(POST, businessWithoutIdBusinessNameRoute).withFormUrlEncodedBody(
          ("value", badBusinessWithoutIdBusinessName)
        )
        val boundForm = form.bind(Map("value" -> badBusinessWithoutIdBusinessName))
        val view      = application.injector.instanceOf[BusinessWithoutIdBusinessNameView]
        val result    = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
        contentAsString(result)      must include("Business name must be 105 characters or less")
      }
    }
    "must return Bad Request & invalidFormat error when BusinessName contains a forward slash" in {
      testInvalidCharacterInBusinessName("Business Name contains /forward slash/")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains an exclamation mark" in {
      testInvalidCharacterInBusinessName("Business Name contains an exclamation mark!")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains a pound sign" in {
      testInvalidCharacterInBusinessName("Business Name contains £ a pound sign")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains a percent sign" in {
      testInvalidCharacterInBusinessName("Business Name contains % a percent sign")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains an asterisk sign" in {
      testInvalidCharacterInBusinessName("Business Name contains * an asterisk sign")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains a bracket sign" in {
      testInvalidCharacterInBusinessName("Business Name contains ( a bracket sign")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains an equals sign" in {
      testInvalidCharacterInBusinessName("Business Name contains = an equals sign")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains a square bracket sign" in {
      testInvalidCharacterInBusinessName("Business Name contains ] a square bracket sign")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains a hash sign" in {
      testInvalidCharacterInBusinessName("Business Name contains # a hash sign")
    }
    "must return Bad Request & invalidFormat error when BusinessName contains an 'at' sign" in {
      testInvalidCharacterInBusinessName("Business Name contains @ an 'at' sign")
    }
  }
}
