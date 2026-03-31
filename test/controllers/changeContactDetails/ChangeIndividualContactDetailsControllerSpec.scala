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

package controllers.changeContactDetails

import base.SpecBase
import models.error.ApiError.{ApplicationError, InternalServerError}
import models.error.DataError
import models.{CheckMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import pages.changeContactDetails.{ChangeDetailsIndividualEmailPage, ChangeDetailsIndividualHavePhonePage, ChangeDetailsIndividualPhoneNumberPage}
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SubscriptionService
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.ChangeDetailsHelper
import viewmodels.govuk.summarylist.*
import views.html.ChangeIndividualContactDetailsView

import java.time.Clock
import scala.concurrent.Future

class ChangeIndividualContactDetailsControllerSpec extends SpecBase {

  def onwardRoute = Call("GET", "/foo")

  lazy val pageRoute: String =
    controllers.changeContactDetails.routes.ChangeIndividualContactDetailsController.onPageLoad().url

  val testRow: SummaryListRow =
    SummaryListRowViewModel(
      key = Key(Text("TEST Key")),
      value = ValueViewModel(Text("TEST Value")),
      actions = Seq(
        ActionItemViewModel(
          Text("TEST Action"),
          controllers.orgWithoutId.routes.HaveTradingNameController.onPageLoad(CheckMode).url
        )
          .withVisuallyHiddenText("TEST HIDDEN TEXT")
      )
    )

  val userAnswersWithEmail: UserAnswers    = emptyUserAnswers.withPage(ChangeDetailsIndividualEmailPage, testEmail)
  val userAnswersWithoutPhone: UserAnswers = emptyUserAnswers
    .withPage(ChangeDetailsIndividualEmailPage, testEmail)
    .withPage(ChangeDetailsIndividualHavePhonePage, true)

  val userAnswersWithoutDisplaySubscription: UserAnswers = emptyUserAnswers
    .withPage(ChangeDetailsIndividualEmailPage, testEmail)
    .withPage(ChangeDetailsIndividualHavePhonePage, true)
    .withPage(ChangeDetailsIndividualPhoneNumberPage, testPhone)

  val fullUserAnswers: UserAnswers = emptyUserAnswers
    .copy(displaySubscriptionResponse = Some(testIndividualDisplaySubscriptionResponse(hasPhone = true)))
    .withPage(ChangeDetailsIndividualEmailPage, testEmail)
    .withPage(ChangeDetailsIndividualHavePhonePage, true)
    .withPage(ChangeDetailsIndividualPhoneNumberPage, testPhone)

  val testBackToManageLink: String =
    "/register-for-cryptoasset-reporting/placeholder?message=Must+redirect+to+service+home+page+%28CARF-411%29"

  "Change Individual Contact Details Controller" - {
    "onPageLoad" - {
      "must return ok with the view when all information is present" in new Setup(
        fullUserAnswers
      ) {
        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        when(mockChangeDetailsHelper.getHasChanged(any(), any(), any())).thenReturn(Right(false))
        when(mockChangeDetailsHelper.decideContinueUrl(any(), any(), any())).thenReturn(None)

        val request                = FakeRequest(GET, pageRoute)
        private val view           = application.injector.instanceOf[ChangeIndividualContactDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)          mustBe OK
        contentAsString(result) mustBe view(Seq(testRow), false, testBackToManageLink)(
          request,
          messages(application)
        ).toString
        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when summary list cannot be constructed" in new Setup(
        emptyUserAnswers
      ) {

        val expectedUrl: String = controllers.routes.PlaceholderController
          .onPageLoad(
            "Should redirect to change contact by phone page"
          )
          .url

        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any())).thenReturn(None)
        when(mockChangeDetailsHelper.getHasChanged(any(), any(), any())).thenReturn(Right(false))
        when(mockChangeDetailsHelper.decideContinueUrl(any(), any(), any())).thenReturn(Some(expectedUrl))

        val request                = FakeRequest(GET, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad(expectedUrl).url
        )
        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when email is missing from user answers" in new Setup(
        emptyUserAnswers
      ) {
        val expectedUrl: String =
          controllers.changeContactDetails.routes.ChangeIndividualEmailController.onPageLoad().url

        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(None)
        when(mockChangeDetailsHelper.getHasChanged(any(), any(), any()))
          .thenReturn(Right(false)) // would return in practise but for test coverage
        when(mockChangeDetailsHelper.decideContinueUrl(any(), any(), any())).thenReturn(Some(expectedUrl))

        val request                = FakeRequest(GET, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad(expectedUrl).url
        )
        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when have phone is missing from user answers" in new Setup(
        userAnswersWithEmail
      ) {

        val expectedUrl: String = controllers.routes.PlaceholderController
          .onPageLoad(
            "Should redirect to change contact by phone page"
          )
          .url

        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(None)
        when(mockChangeDetailsHelper.getHasChanged(any(), any(), any()))
          .thenReturn(Right(false)) // would return in practise but for test coverage
        when(mockChangeDetailsHelper.decideContinueUrl(any(), any(), any())).thenReturn(Some(expectedUrl))

        val request                = FakeRequest(GET, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad(expectedUrl).url
        )
        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when phone is missing from user answers" in new Setup(
        userAnswersWithoutPhone
      ) {

        val expectedUrl: String = controllers.routes.PlaceholderController
          .onPageLoad(
            "Should redirect to change contact by phone page"
          )
          .url

        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(None)
        when(mockChangeDetailsHelper.getHasChanged(any(), any(), any()))
          .thenReturn(Right(false)) // would return in practise but for test coverage
        when(mockChangeDetailsHelper.decideContinueUrl(any(), any(), any())).thenReturn(Some(expectedUrl))

        val request                = FakeRequest(GET, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad(expectedUrl).url
        )
        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to Journey Recovery page when displaySubscriptionResponse is missing from user answers so hasChanged is Left" in new Setup(
        userAnswersWithoutDisplaySubscription
      ) {

        val expectedUrl: String = controllers.routes.PlaceholderController
          .onPageLoad(
            "Should redirect to change contact by phone page"
          )
          .url

        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        when(mockChangeDetailsHelper.getHasChanged(any(), any(), any()))
          .thenReturn(Left(ApplicationError)) // would return in practise but for test coverage)
        when(mockChangeDetailsHelper.decideContinueUrl(any(), any(), any())).thenReturn(Some(expectedUrl))

        val request                = FakeRequest(GET, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)

        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
      }
    }

    "onSubmit" - {
      "must redirect to success page if update subscription is successful" in new Setup(emptyUserAnswers) {
        when(mockSubscriptionService.updateSubscription(any())(any()))
          .thenReturn(Future.successful(Right((): Unit)))

        val request                = FakeRequest(POST, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.changeContactDetails.routes.ChangeDetailsUpdatedController
          .onPageLoad()
          .url

        verify(mockSubscriptionService, times(1)).updateSubscription(any())(any())
      }
      "must redirect to journey recovery if update subscription is unsuccessful" in new Setup(emptyUserAnswers) {
        when(mockSubscriptionService.updateSubscription(any())(any()))
          .thenReturn(Future.successful(Left(InternalServerError)))

        val request                = FakeRequest(POST, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockSubscriptionService, times(1)).updateSubscription(any())(any())
      }
    }
  }

  class Setup(userAnswers: UserAnswers) {
    final val mockSubscriptionService = mock[SubscriptionService]
    final val mockChangeDetailsHelper = mock[ChangeDetailsHelper]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SubscriptionService].toInstance(mockSubscriptionService),
          bind[ChangeDetailsHelper].toInstance(mockChangeDetailsHelper),
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[Clock].toInstance(clock)
        )
        .build()
  }

}
