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

package controllers.individual

import base.SpecBase
import controllers.routes
import forms.individual.RegisterDateOfBirthFormProvider
import models.error.ApiError
import models.error.ApiError.NotFoundError
import models.responses.AddressRegistrationResponse
import models.{IndividualDetails, Name, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.{NiNumberPage, RegisterDateOfBirthPage, WhatIsYourNameIndividualPage}
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.RegistrationService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.individual.RegisterDateOfBirthView

import java.time.LocalDate
import scala.concurrent.Future

class RegisterDateOfBirthControllerSpec extends SpecBase with MockitoSugar {

  private implicit val messages: Messages = stubMessages()
  private val formProvider                = new RegisterDateOfBirthFormProvider()
  private def form                        = formProvider()
  def onwardRoute                         = Call("GET", "/foo")

  private val validBirthDate: LocalDate = LocalDate.of(2000, 1, 1)
  private val validNino                 = "JX123456D"
  private val validName                 = Name("firstName example", "lastName example")

  val validIndividualDetails = IndividualDetails(
    safeId = "X12345",
    firstName = "John",
    lastName = "Doe",
    middleName = None,
    address = AddressRegistrationResponse( // reg
      addressLine1 = "123 Main Street",
      addressLine2 = Some("Birmingham"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("B23 2AZ"),
      countryCode = "GB"
    )
  )

  private lazy val registerDateOfBirthRoute =
    controllers.individual.routes.RegisterDateOfBirthController.onPageLoad(NormalMode).url
  override val emptyUserAnswers             = UserAnswers(userAnswersId)

  private def buildGetRequest(
      userAnswers: Option[UserAnswers] = Some(emptyUserAnswers)
  ): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest("GET", registerDateOfBirthRoute)

  private def buildPostRequest(
      userAnswers: Option[UserAnswers] = Some(emptyUserAnswers),
      day: String = validBirthDate.getDayOfMonth.toString,
      month: String = validBirthDate.getMonthValue.toString,
      year: String = validBirthDate.getYear.toString,
      invalid: Boolean = false
  ): FakeRequest[AnyContentAsFormUrlEncoded] = {
    val formData =
      if (invalid) { Map("value" -> "invalid value") }
      else {
        Map(
          "value.day"   -> day,
          "value.month" -> month,
          "value.year"  -> year
        )
      }
    FakeRequest("POST", registerDateOfBirthRoute).withFormUrlEncodedBody(formData.toSeq: _*)
  }

  private def buildUserAnswers(
      nino: Option[String] = None,
      name: Option[Name] = None,
      dob: Option[LocalDate] = None
  ): UserAnswers = {
    val base     = UserAnswers(userAnswersId)
    val withNino = nino.fold(base)(n => base.set(NiNumberPage, n).success.value)
    val withName = name.fold(withNino)(n => withNino.set(WhatIsYourNameIndividualPage, n).success.value)
    dob.fold(withName)(d => withName.set(RegisterDateOfBirthPage, d).success.value)
  }

  "RegisterDateOfBirth Controller" - {
    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(buildUserAnswers())).build()
      running(application) {
        val result = route(application, buildGetRequest()).value
        val view   = application.injector.instanceOf[RegisterDateOfBirthView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(buildGetRequest(), messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val application = applicationBuilder(userAnswers = Some(buildUserAnswers(dob = Some(validBirthDate)))).build()
      running(application) {
        val view   = application.injector.instanceOf[RegisterDateOfBirthView]
        val result = route(application, buildGetRequest()).value

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validBirthDate), NormalMode)(
          buildGetRequest(),
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted and the service returns valid IndividualDetails" in {
      val mockRegistrationService = mock[RegistrationService]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockRegistrationService.getIndividualByNino(any[String], any[Name], any[LocalDate])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validIndividualDetails)))

      val application =
        applicationBuilder(userAnswers = Some(buildUserAnswers(nino = Some(validNino), name = Some(validName))))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[RegistrationService].toInstance(mockRegistrationService)
          )
          .build()

      running(application) {
        val result = route(application, buildPostRequest()).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to CouldNotConfirmIdentity page when the service returns NotFoundError" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val mockRegistrationService = mock[RegistrationService]
      when(
        mockRegistrationService.getIndividualByNino(any[String], any[Name], any[LocalDate])(any[HeaderCarrier])
      ).thenReturn(Future.successful(Left(NotFoundError)))

      val application = applicationBuilder(userAnswers =
        Some(buildUserAnswers(nino = Some(validNino), name = Some(validName), dob = Some(validBirthDate)))
      )
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[RegistrationService].toInstance(mockRegistrationService)
        )
        .build()

      running(application) {
        val result = route(application, buildPostRequest()).value
        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value        mustEqual controllers.individualWithoutId.routes.IndWithoutNinoCouldNotConfirmIdentityController
          .onPageLoad()
          .url
      }
    }

    "must redirect to Journey Recovery when the service returns InternalServerError" in {
      val mockRegistrationService = mock[RegistrationService]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(
        mockRegistrationService.getIndividualByNino(any[String], any[Name], any[LocalDate])(any[HeaderCarrier])
      ).thenReturn(Future.successful(Left(ApiError.InternalServerError)))

      val application = applicationBuilder(userAnswers =
        Some(buildUserAnswers(nino = Some(validNino), name = Some(validName), dob = Some(validBirthDate)))
      ).overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[RegistrationService].toInstance(mockRegistrationService)
      ).build()

      running(application) {
        val result = route(application, buildPostRequest()).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when user answers exists, but nino is missing" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val mockRegistrationService = mock[RegistrationService]
      when(
        mockRegistrationService.getIndividualByNino(any[String], any[Name], any[LocalDate])(any[HeaderCarrier])
      ).thenReturn(Future.successful(Left(ApiError.InternalServerError)))

      val application = applicationBuilder(userAnswers =
        Some(buildUserAnswers(nino = None, name = Some(validName), dob = Some(validBirthDate)))
      ).overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[RegistrationService].toInstance(mockRegistrationService)
      ).build()

      running(application) {
        val result = route(application, buildPostRequest()).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when user answers exists, but name is missing" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val mockRegistrationService = mock[RegistrationService]
      when(
        mockRegistrationService.getIndividualByNino(any[String], any[Name], any[LocalDate])(any[HeaderCarrier])
      ).thenReturn(Future.successful(Left(ApiError.InternalServerError)))

      val application = applicationBuilder(userAnswers =
        Some(buildUserAnswers(nino = Some(validNino), name = None, dob = Some(validBirthDate)))
      ).overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[RegistrationService].toInstance(mockRegistrationService)
      ).build()

      running(application) {
        val result = route(application, buildPostRequest()).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when user answers exists, but DoB is missing" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val mockRegistrationService = mock[RegistrationService]
      when(
        mockRegistrationService.getIndividualByNino(any[String], any[Name], any[LocalDate])(any[HeaderCarrier])
      ).thenReturn(Future.successful(Left(ApiError.InternalServerError)))

      val application = applicationBuilder(userAnswers =
        Some(buildUserAnswers(nino = Some(validNino), name = Some(validName), dob = None))
      ).overrides(
        bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
        bind[RegistrationService].toInstance(mockRegistrationService)
      ).build()

      running(application) {
        val result = route(application, buildPostRequest()).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(buildUserAnswers())).build()
      running(application) {
        val boundForm = form.bind(Map("value" -> "invalid value"))
        val view      = application.injector.instanceOf[RegisterDateOfBirthView]
        val result    = route(application, buildPostRequest(invalid = true)).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(
          buildPostRequest(invalid = true),
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val result = route(application, buildGetRequest(userAnswers = None)).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val result = route(application, buildPostRequest(userAnswers = None)).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
