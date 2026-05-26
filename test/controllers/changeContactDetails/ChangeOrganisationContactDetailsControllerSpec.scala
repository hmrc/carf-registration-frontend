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

import models.error.ApiError.InternalServerError
import models.{CheckMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import play.api.Application
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SubscriptionService
import testUtils.ChangeDetailsTestData
import types.ResultT
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.ChangeOrganisationDetailsHelper
import viewmodels.govuk.summarylist.*
import views.html.ChangeOrganisationContactDetailsView

import java.time.Clock
import scala.concurrent.Future

class ChangeOrganisationContactDetailsControllerSpec extends ChangeDetailsTestData {

  def onwardRoute = Call("GET", "/foo")

  lazy val pageRoute: String =
    controllers.changeContactDetails.routes.ChangeOrganisationContactDetailsController.onPageLoad().url

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

  val testBackToManageLink: String =
    "/register-for-cryptoasset-reporting/placeholder?message=Must+redirect+to+service+home+page+%28CARF-411%29"

  "Change Organisation Contact Details Controller" - {
    "onPageLoad" - {
      "must return ok with the view when all information is present" in new Setup(fullUserAnswers) {
        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        when(mockChangeDetailsHelper.getSecondContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        val request                = FakeRequest(GET, pageRoute)
        private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)          mustBe OK
        contentAsString(result) mustBe view(Seq(testRow), Seq(testRow), true, testBackToManageLink)(
          request,
          messages(application)
        ).toString

        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
        verify(mockChangeDetailsHelper, times(1)).getSecondContactDetailsSectionMaybe(any())(any())
      }

      "must return ok with the view when all information is present when have phone is false" in new Setup(
        userAnswersNoPhone
      ) {
        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        when(mockChangeDetailsHelper.getSecondContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        val request                = FakeRequest(GET, pageRoute)
        private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)          mustBe OK
        contentAsString(result) mustBe view(Seq(testRow), Seq(testRow), true, testBackToManageLink)(
          request,
          messages(application)
        ).toString

        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
        verify(mockChangeDetailsHelper, times(1)).getSecondContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when summary list cannot be constructed" in new Setup(
        userAnswersNameMissing
      ) {
        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(None)

        val request                = FakeRequest(GET, pageRoute)
        private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url
        )

        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
        verify(mockChangeDetailsHelper, never()).getSecondContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when name is missing from user answers" in new Setup(
        userAnswersNameMissing
      ) {
        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        val request                = FakeRequest(GET, pageRoute)
        private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url
        )

        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
        verify(mockChangeDetailsHelper, never()).getSecondContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when have phone is missing from user answers" in new Setup(
        userAnswersWithoutHavePhone
      ) {
        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        val request                = FakeRequest(GET, pageRoute)
        private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url
        )

        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
        verify(mockChangeDetailsHelper, never()).getSecondContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when second contact is missing from user answers" in
        new Setup(fullFirstContactUserAnswersWithSecondContactMissing) {

          when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
            .thenReturn(Some(Seq(testRow)))

          when(mockChangeDetailsHelper.getSecondContactDetailsSectionMaybe(any())(any()))
            .thenReturn(Some(Seq(testRow)))

          val request                = FakeRequest(GET, pageRoute)
          private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)           mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url
          )

          verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
          verify(mockChangeDetailsHelper, times(1)).getSecondContactDetailsSectionMaybe(any())(any())
        }

      "must redirect to some details are missing page when second summary list cannot be constructed" in new Setup(
        fullUserAnswers
      ) {
        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        when(mockChangeDetailsHelper.getSecondContactDetailsSectionMaybe(any())(any()))
          .thenReturn(None)

        val request                = FakeRequest(GET, pageRoute)
        private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url
        )
        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
        verify(mockChangeDetailsHelper, times(1)).getSecondContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when second have phone is missing from user answers" in new Setup(
        userAnswersSecondContactWithoutHavePhone
      ) {
        when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        when(mockChangeDetailsHelper.getSecondContactDetailsSectionMaybe(any())(any()))
          .thenReturn(Some(Seq(testRow)))

        val request                = FakeRequest(GET, pageRoute)
        private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(
          controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url
        )

        verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
        verify(mockChangeDetailsHelper, times(1)).getSecondContactDetailsSectionMaybe(any())(any())
      }

      "must redirect to some details are missing page when second name is missing from user answers" in
        new Setup(userAnswersSecondContactNameMissing) {
          when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
            .thenReturn(Some(Seq(testRow)))

          when(mockChangeDetailsHelper.getSecondContactDetailsSectionMaybe(any())(any()))
            .thenReturn(Some(Seq(testRow)))

          val request                = FakeRequest(GET, pageRoute)
          private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)           mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url
          )

          verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
          verify(mockChangeDetailsHelper, times(1)).getSecondContactDetailsSectionMaybe(any())(any())
        }

      "must redirect to some details are missing page when second phone is missing from user answers" in
        new Setup(userAnswersSecondPhoneMissing) {
          when(mockChangeDetailsHelper.getFirstContactDetailsSectionMaybe(any())(any()))
            .thenReturn(Some(Seq(testRow)))

          when(mockChangeDetailsHelper.getSecondContactDetailsSectionMaybe(any())(any()))
            .thenReturn(Some(Seq(testRow)))

          val request                = FakeRequest(GET, pageRoute)
          private val view           = application.injector.instanceOf[ChangeOrganisationContactDetailsView]
          val result: Future[Result] = route(application, request).value

          status(result)           mustBe SEE_OTHER
          redirectLocation(result) mustBe Some(
            controllers.changeContactDetails.routes.ContactDetailsMissingController.onPageLoad().url
          )

          verify(mockChangeDetailsHelper, times(1)).getFirstContactDetailsSectionMaybe(any())(any())
          verify(mockChangeDetailsHelper, times(1)).getSecondContactDetailsSectionMaybe(any())(any())
        }

      "must redirect to Journey Recovery page when displaySubscriptionResponse is missing from user answers so hasChanged is None" in new Setup(
        userAnswersWithoutDisplay
      ) {

        val request                = FakeRequest(GET, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result)           mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.JourneyRecoveryController.onPageLoad().url)

      }
    }

    "onSubmit" - {
      "must redirect to success page if update subscription is successful" in new Setup(emptyUserAnswers) {
        when(mockSubscriptionService.updateSubscription(any[UserAnswers])(any(), any()))
          .thenReturn(ResultT.fromValue(testSubscriptionId))

        when(mockSessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        val request                = FakeRequest(POST, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.changeContactDetails.routes.ChangeDetailsUpdatedController
          .onPageLoad()
          .url

        verify(mockSubscriptionService, times(1)).updateSubscription(any[UserAnswers])(any(), any())
      }
      "must redirect to journey recovery if update subscription is unsuccessful" in new Setup(emptyUserAnswers) {
        when(mockSubscriptionService.updateSubscription(any[UserAnswers])(any(), any()))
          .thenReturn(ResultT.fromError(InternalServerError))

        val request                = FakeRequest(POST, pageRoute)
        val result: Future[Result] = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockSubscriptionService, times(1)).updateSubscription(any[UserAnswers])(any(), any())
      }
      "must redirect to journey recovery if no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()
        running(application) {
          val request = FakeRequest(POST, pageRoute)

          val result = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }

  class Setup(userAnswers: UserAnswers) {
    import play.api.inject.bind

    final val mockSubscriptionService = mock[SubscriptionService]
    final val mockChangeDetailsHelper = mock[ChangeOrganisationDetailsHelper]

    val application: Application =
      applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SubscriptionService].toInstance(mockSubscriptionService),
          bind[ChangeOrganisationDetailsHelper].toInstance(mockChangeDetailsHelper),
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[Clock].toInstance(clock)
        )
        .build()
  }
}
