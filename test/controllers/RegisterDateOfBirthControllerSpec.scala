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

import java.time.{LocalDate, ZoneOffset}
import base.SpecBase
import forms.RegisterDateOfBirthFormProvider
import models.{Address, IndividualDetails, Name, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.mockito.MockitoSugar
import pages.{NiNumberPage, RegisterDateOfBirthPage, WhatIsYourNameIndividualPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.RegistrationService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.RegisterDateOfBirthView

import scala.concurrent.Future

class RegisterDateOfBirthControllerSpec extends SpecBase with MockitoSugar {
  private implicit val messages: Messages = stubMessages()
  private val formProvider                = new RegisterDateOfBirthFormProvider()
  private def form                        = formProvider()
  def onwardRoute                         = Call("GET", "/foo")

  private val validDateAnswer: LocalDate = LocalDate.of(2000, 1, 1)
  private val validNino                  = "JX123456D"
  private val validName                  = Name("firstName example", "lastName example")
  private val validUserAnswers           = UserAnswers(userAnswersId)
    .set(NiNumberPage, validNino)
    .success
    .value
    .set(WhatIsYourNameIndividualPage, validName)
    .success
    .value
    .set(RegisterDateOfBirthPage, validDateAnswer)
    .success
    .value

  val validIndividualDetails = IndividualDetails(
    safeId = "X12345",
    firstName = "John",
    lastName = "Doe",
    middleName = None,
    address = Address(
      addressLine1 = "123 Main Street",
      addressLine2 = Some("Birmingham"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("B23 2AZ"),
      countryCode = "GB"
    )
  )

  private lazy val registerDateOfBirthRoute = routes.RegisterDateOfBirthController.onPageLoad(NormalMode).url
  override val emptyUserAnswers             = UserAnswers(userAnswersId)

  def getRequest(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, registerDateOfBirthRoute)

  def postRequest(): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, registerDateOfBirthRoute)
      .withFormUrlEncodedBody(
        "value.day"   -> validDateAnswer.getDayOfMonth.toString,
        "value.month" -> validDateAnswer.getMonthValue.toString,
        "value.year"  -> validDateAnswer.getYear.toString
      )

  "RegisterDateOfBirth Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val result = route(application, getRequest()).value
        val view   = application.injector.instanceOf[RegisterDateOfBirthView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(getRequest(), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val application = applicationBuilder(userAnswers = Some(validUserAnswers)).build()

      running(application) {
        val view   = application.injector.instanceOf[RegisterDateOfBirthView]
        val result = route(application, getRequest()).value
        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validDateAnswer), NormalMode)(
          getRequest(),
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted and the service returns 200 OK" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val mockRegistrationService = mock[RegistrationService]
      when(
        mockRegistrationService.getIndividualByNino(any[String], any[UserAnswers])(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(validIndividualDetails)))

      val userAnswersWithValidNino = UserAnswers(userAnswersId)
        .set(NiNumberPage, "JX123456D")
        .success
        .value
        .set(WhatIsYourNameIndividualPage, validName)
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithValidNino))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[RegistrationService].toInstance(mockRegistrationService)
          )
          .build()

      val request =
        FakeRequest(POST, registerDateOfBirthRoute)
          .withFormUrlEncodedBody(
            "value.day"   -> validDateAnswer.getDayOfMonth.toString,
            "value.month" -> validDateAnswer.getMonthValue.toString,
            "value.year"  -> validDateAnswer.getYear.toString
          )
      running(application) {
        val result = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to CouldNotConfirmIdentity page when the service returns 400 (no individual found)" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val mockRegistrationService = mock[RegistrationService]
      when(
        mockRegistrationService.getIndividualByNino(eqTo("XX123456D"), any())(any[HeaderCarrier])
      ).thenReturn(Future.successful(None))

      val userAnswersWithInvalidNino = validUserAnswers.set(NiNumberPage, "XX123456D").success.value

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithInvalidNino))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[RegistrationService].toInstance(mockRegistrationService)
          )
          .build()

      val request =
        FakeRequest(POST, registerDateOfBirthRoute)
          .withFormUrlEncodedBody(
            "value.day"   -> validDateAnswer.getDayOfMonth.toString,
            "value.month" -> validDateAnswer.getMonthValue.toString,
            "value.year"  -> validDateAnswer.getYear.toString
          )

      running(application) {
        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.IndWithoutNinoCouldNotConfirmIdentityController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      val request     =
        FakeRequest(POST, registerDateOfBirthRoute)
          .withFormUrlEncodedBody(("value", "invalid value"))

      running(application) {
        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view      = application.injector.instanceOf[RegisterDateOfBirthView]
        val result    = route(application, request).value
        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, getRequest()).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val result = route(application, postRequest()).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
