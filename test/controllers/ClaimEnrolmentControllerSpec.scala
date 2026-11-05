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
import config.{ClaimEnrolmentInformation, ClaimEnrolmentInformationProvider, FrontendAppConfig}
import controllers.actions.FakeCtUtrRetrievalAction
import forms.ClaimEnrolmentFormProvider
import models.error.ApiError.InternalServerError
import models.error.{ApiError, DataError}
import models.responses.AddressRegistrationResponse
import models.{BusinessDetails, UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import pages.PasscodePage
import play.api.Application
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{EnrolmentService, RegistrationService}
import types.ResultT
import views.html.ClaimEnrolmentView

import java.time.Clock
import scala.concurrent.Future

class ClaimEnrolmentControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new ClaimEnrolmentFormProvider()
  val form         = formProvider()

  val mockRegistrationService: RegistrationService                             = mock[RegistrationService]
  val mockAppConfig: FrontendAppConfig                                         = mock[FrontendAppConfig]
  val mockEnrolmentService: EnrolmentService                                   = mock[EnrolmentService]
  val mockClaimEnrolmentInformationProvider: ClaimEnrolmentInformationProvider = mock[ClaimEnrolmentInformationProvider]

  lazy val claimEnrolmentRoute = routes.ClaimEnrolmentController.onPageLoad().url

  val testBusinessName                      = "Test Business Ltd"
  val businessTestBusiness: BusinessDetails = BusinessDetails(
    name = testBusinessName,
    address =
      AddressRegistrationResponse("123 Test Street", Some("Birmingham"), None, None, Some("B23 2AZ"), "GB", None),
    safeId = testSafeId
  )

  val testClaimEnrolmentInformation: ClaimEnrolmentInformation =
    ClaimEnrolmentInformation(safeId = testSafeId, carfId = testSubscriptionId, isAbroad = true)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockRegistrationService, mockEnrolmentService, mockClaimEnrolmentInformationProvider)
    when(mockAppConfig.feedbackUrl(any())).thenReturn("foo")
  }

  "ClaimEnrolmentController" - {
    "must return OK and the correct view for a GET when a UTR exists in the request and the registration call is successful" in new Setup(
      requestUtr = Some(testUtrString)
    ) {
      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Right(businessTestBusiness)))

      running(application) {
        val request = FakeRequest(GET, claimEnrolmentRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ClaimEnrolmentView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, testBusinessName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET when no UTR exists in the request" in new Setup(
      requestUtr = None
    ) {

      running(application) {
        val request = FakeRequest(GET, claimEnrolmentRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when a UTR exists in the request but the registration call is unsuccessful" in new Setup(
      requestUtr = Some(testUtrString)
    ) {
      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Left(ApiError.InternalServerError)))

      running(application) {
        val request = FakeRequest(GET, claimEnrolmentRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must create an enrolment and redirect to the next page when valid data is submitted and the passcode feature switch is enabled" in new Setup(
      requestUtr = Some(testUtrString),
      userAnswers = Some(emptyUserAnswers.withPage(PasscodePage, "123"))
    ) {

      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Right(businessTestBusiness)))
      when(mockClaimEnrolmentInformationProvider.claimEnrolmentInformation).thenReturn(
        Some(Seq(testClaimEnrolmentInformation))
      )
      when(mockAppConfig.passcodeCheckEnabled).thenReturn(true)
      when(mockAppConfig.managementRoutingUrl).thenReturn("/url")
      when(
        mockEnrolmentService.enrol(
          eqTo(testClaimEnrolmentInformation.carfId),
          eqTo(businessTestBusiness.address.postalCode),
          eqTo(testClaimEnrolmentInformation.isAbroad)
        )(any(), any())
      ).thenReturn(ResultT.fromValue((): Unit))

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual "/url/passcode/123"
      }
    }

    "must create an enrolment and redirect to the next page when valid data is submitted and the passcode feature switch is disabled" in new Setup(
      requestUtr = Some(testUtrString)
    ) {

      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Right(businessTestBusiness)))
      when(mockClaimEnrolmentInformationProvider.claimEnrolmentInformation).thenReturn(
        Some(Seq(testClaimEnrolmentInformation))
      )
      when(mockAppConfig.passcodeCheckEnabled).thenReturn(false)
      when(mockAppConfig.managementRoutingUrl).thenReturn("/url")
      when(
        mockEnrolmentService.enrol(
          eqTo(testClaimEnrolmentInformation.carfId),
          eqTo(businessTestBusiness.address.postalCode),
          eqTo(testClaimEnrolmentInformation.isAbroad)
        )(any(), any())
      ).thenReturn(ResultT.fromValue((): Unit))

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual "/url"
      }
    }

    "must redirect to Journey Recovery when no claim enrolment info can be found" in new Setup(
      requestUtr = Some(testUtrString)
    ) {

      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Right(businessTestBusiness)))
      when(mockClaimEnrolmentInformationProvider.claimEnrolmentInformation).thenReturn(
        None
      )

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when register with id returns an error" in new Setup(
      requestUtr = Some(testUtrString)
    ) {

      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Left(InternalServerError)))
      when(mockClaimEnrolmentInformationProvider.claimEnrolmentInformation).thenReturn(
        Some(Seq(testClaimEnrolmentInformation))
      )

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the user clicks 'No' to the page answer" in new Setup(
      requestUtr = Some(testUtrString)
    ) {

      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Right(businessTestBusiness)))
      when(mockClaimEnrolmentInformationProvider.claimEnrolmentInformation).thenReturn(
        Some(Seq.empty)
      )

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when no matching claim enrolment info can be found" in new Setup(
      requestUtr = Some(testUtrString)
    ) {

      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Right(businessTestBusiness)))
      when(mockClaimEnrolmentInformationProvider.claimEnrolmentInformation).thenReturn(
        Some(Seq.empty)
      )

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the enrolment call fails" in new Setup(
      requestUtr = Some(testUtrString)
    ) {

      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Right(businessTestBusiness)))
      when(mockClaimEnrolmentInformationProvider.claimEnrolmentInformation).thenReturn(
        Some(Seq(testClaimEnrolmentInformation))
      )
      when(
        mockEnrolmentService.enrol(
          eqTo(testClaimEnrolmentInformation.carfId),
          eqTo(businessTestBusiness.address.postalCode),
          eqTo(testClaimEnrolmentInformation.isAbroad)
        )(any(), any())
      ).thenReturn(ResultT.fromError(DataError))

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in new Setup(
      requestUtr = Some(testUtrString)
    ) {
      when(mockRegistrationService.getBusinessWithUtr(any(), eqTo(testUtrString))(any()))
        .thenReturn(Future.successful(Right(businessTestBusiness)))
      when(mockClaimEnrolmentInformationProvider.claimEnrolmentInformation).thenReturn(
        Some(Seq(testClaimEnrolmentInformation))
      )

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ClaimEnrolmentView]

        val result = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, testBusinessName)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in new Setup(
      requestUtr = Some(testUtrString),
      userAnswers = None
    ) {
      when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction(utr = None))

      running(application) {
        val request = FakeRequest(GET, claimEnrolmentRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in new Setup(
      requestUtr = Some(testUtrString),
      userAnswers = None
    ) {
      when(mockCtUtrRetrievalAction.apply()).thenReturn(new FakeCtUtrRetrievalAction(utr = None))

      running(application) {
        val request =
          FakeRequest(POST, claimEnrolmentRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  class Setup(
      requestUtr: Option[String] = None,
      userAnswers: Option[UserAnswers] = Some(emptyUserAnswers)
  ) {
    when(mockCtUtrRetrievalAction.apply())
      .thenReturn(new FakeCtUtrRetrievalAction(requestUtr.map(UniqueTaxpayerReference(_))))

    val application: Application =
      applicationBuilder(requestUtr = requestUtr, userAnswers = userAnswers)
        .overrides(
          bind[RegistrationService].toInstance(mockRegistrationService),
          bind[ClaimEnrolmentInformationProvider].toInstance(mockClaimEnrolmentInformationProvider),
          bind[EnrolmentService].toInstance(mockEnrolmentService),
          bind[FrontendAppConfig].toInstance(mockAppConfig),
          bind[Clock].toInstance(clock)
        )
        .build()
  }
}
