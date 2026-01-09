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
import models.JourneyType.{IndWithNino, OrgWithUtr}
import models.error.ApiError.InternalServerError
import models.{CheckMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import play.api.Application
import play.api.inject.bind
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SubscriptionService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}
import utils.CheckYourAnswersHelper
import viewmodels.Section
import viewmodels.govuk.summarylist.*
import views.html.CheckYourAnswersView

import java.time.Clock
import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends SpecBase {

  val orgWithUtrUserAnswers: UserAnswers  = emptyUserAnswers.copy(journeyType = Some(OrgWithUtr))
  val indWithNinoUserAnswers: UserAnswers = emptyUserAnswers.copy(journeyType = Some(IndWithNino))
  lazy val cyaRoute: String               = routes.CheckYourAnswersController.onPageLoad().url
  def onwardRoute                         = Call("GET", "/foo")

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

  val testSection: Section = Section("TEST SECTION NAME", Seq(testRow))

  "Check Your Answers Controller" - {
    "onPageLoad" - {
      "when journey is org with utr" - {
        "must return OK and the correct view for a GET when all answers have been answered as expected" in new Setup(
          AffinityGroup.Organisation,
          orgWithUtrUserAnswers
        ) {
          when(mockCheckYourAnswersHelper.getBusinessDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))
          when(mockCheckYourAnswersHelper.getFirstContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))
          when(mockCheckYourAnswersHelper.getSecondContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                    = FakeRequest(GET, cyaRoute)
          val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]
          val result: Future[Result]     = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(Seq(testSection, testSection, testSection))(
            request,
            messages(application)
          ).toString
        }

        "must redirect to information missing page for a GET when business details are missing" in new Setup(
          AffinityGroup.Organisation,
          orgWithUtrUserAnswers
        ) {
          when(mockCheckYourAnswersHelper.getBusinessDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(None)
          when(mockCheckYourAnswersHelper.getFirstContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))
          when(mockCheckYourAnswersHelper.getSecondContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                    = FakeRequest(GET, cyaRoute)
          val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]
          val result: Future[Result]     = route(application, request).value

          status(result)               mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.InformationMissingController.onPageLoad().url
          verify(mockCheckYourAnswersHelper, times(1)).getBusinessDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
          verify(mockCheckYourAnswersHelper, times(1)).getFirstContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
          verify(mockCheckYourAnswersHelper, times(1)).getSecondContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
        }

        "must redirect to information missing page for a GET when any required first contact details are missing" in new Setup(
          AffinityGroup.Organisation,
          orgWithUtrUserAnswers
        ) {
          when(mockCheckYourAnswersHelper.getBusinessDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))
          when(mockCheckYourAnswersHelper.getFirstContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(None)
          when(mockCheckYourAnswersHelper.getSecondContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                    = FakeRequest(GET, cyaRoute)
          val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]
          val result: Future[Result]     = route(application, request).value

          status(result)               mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.InformationMissingController.onPageLoad().url
          verify(mockCheckYourAnswersHelper, times(1)).getBusinessDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
          verify(mockCheckYourAnswersHelper, times(1)).getFirstContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
          verify(mockCheckYourAnswersHelper, times(1)).getSecondContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
        }

        "must redirect to information missing page for a GET when any required second contact details are missing" in new Setup(
          AffinityGroup.Organisation,
          orgWithUtrUserAnswers
        ) {
          when(mockCheckYourAnswersHelper.getBusinessDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))
          when(mockCheckYourAnswersHelper.getFirstContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(Some(testSection))
          when(mockCheckYourAnswersHelper.getSecondContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(any()))
            .thenReturn(None)

          val request                    = FakeRequest(GET, cyaRoute)
          val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]
          val result: Future[Result]     = route(application, request).value

          status(result)               mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.InformationMissingController.onPageLoad().url
          verify(mockCheckYourAnswersHelper, times(1)).getBusinessDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
          verify(mockCheckYourAnswersHelper, times(1)).getFirstContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
          verify(mockCheckYourAnswersHelper, times(1)).getSecondContactDetailsSectionMaybe(eqTo(orgWithUtrUserAnswers))(
            any()
          )
        }
      }

      "when journey is individual with NINO" - {
        "must return OK and the correct view for a GET when all answers have been answered as expected" in new Setup(
          AffinityGroup.Individual,
          indWithNinoUserAnswers
        ) {
          when(mockCheckYourAnswersHelper.indWithNinoYourDetailsMaybe(eqTo(indWithNinoUserAnswers))(any()))
            .thenReturn(Some(testSection))
          when(mockCheckYourAnswersHelper.indContactDetailsMaybe(eqTo(indWithNinoUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                    = FakeRequest(GET, cyaRoute)
          val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]
          val result: Future[Result]     = route(application, request).value

          status(result)          mustEqual OK
          contentAsString(result) mustEqual view(Seq(testSection, testSection))(
            request,
            messages(application)
          ).toString
        }

        "must redirect to information missing page for a GET when 'your details' data is missing" in new Setup(
          AffinityGroup.Individual,
          indWithNinoUserAnswers
        ) {
          when(mockCheckYourAnswersHelper.indWithNinoYourDetailsMaybe(eqTo(indWithNinoUserAnswers))(any()))
            .thenReturn(None)
          when(mockCheckYourAnswersHelper.indContactDetailsMaybe(eqTo(indWithNinoUserAnswers))(any()))
            .thenReturn(Some(testSection))

          val request                    = FakeRequest(GET, cyaRoute)
          val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]
          val result: Future[Result]     = route(application, request).value

          status(result)               mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.InformationMissingController.onPageLoad().url
          verify(mockCheckYourAnswersHelper, times(1)).indWithNinoYourDetailsMaybe(eqTo(indWithNinoUserAnswers))(
            any()
          )
          verify(mockCheckYourAnswersHelper, times(1)).indContactDetailsMaybe(eqTo(indWithNinoUserAnswers))(
            any()
          )
        }

        "must redirect to information missing page for a GET when any required first contact details are missing" in new Setup(
          AffinityGroup.Individual,
          indWithNinoUserAnswers
        ) {
          when(mockCheckYourAnswersHelper.indWithNinoYourDetailsMaybe(eqTo(indWithNinoUserAnswers))(any()))
            .thenReturn(Some(testSection))
          when(mockCheckYourAnswersHelper.indContactDetailsMaybe(eqTo(indWithNinoUserAnswers))(any()))
            .thenReturn(None)

          val request                    = FakeRequest(GET, cyaRoute)
          val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]
          val result: Future[Result]     = route(application, request).value

          status(result)               mustEqual SEE_OTHER
          redirectLocation(result).get mustEqual routes.InformationMissingController.onPageLoad().url
          verify(mockCheckYourAnswersHelper, times(1)).indWithNinoYourDetailsMaybe(eqTo(indWithNinoUserAnswers))(
            any()
          )
          verify(mockCheckYourAnswersHelper, times(1)).indContactDetailsMaybe(eqTo(indWithNinoUserAnswers))(
            any()
          )
        }
      }
    }

    "onSubmit" - {
      "when the service call is successful" - {
        "must redirect to the confirmation page" in new Setup(AffinityGroup.Organisation, orgWithUtrUserAnswers) {
          when(mockSubscriptionService.subscribe()).thenReturn(Future.successful(Right("Success!")))

          val request                = FakeRequest(POST, cyaRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.PlaceholderController
            .onPageLoad("Should redirect to confirmation page /confirm-registration (CARF-259)")
            .url

        }
      }
      "when the service call is NOT successful" - {
        "must redirect to the journey recovery page" in new Setup(AffinityGroup.Organisation, orgWithUtrUserAnswers) {
          when(mockSubscriptionService.subscribe()).thenReturn(Future.successful(Left(InternalServerError)))

          val request                = FakeRequest(POST, cyaRoute)
          val result: Future[Result] = route(application, request).value

          status(result)                 mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url

        }
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(GET, cyaRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()
      running(application) {
        val request = FakeRequest(POST, cyaRoute)

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

  }

  class Setup(
      affinityGroup: AffinityGroup,
      userAnswers: UserAnswers
  ) {
    final val mockSubscriptionService    = mock[SubscriptionService]
    final val mockCheckYourAnswersHelper = mock[CheckYourAnswersHelper]

    val application: Application =
      applicationBuilder(affinityGroup = affinityGroup, userAnswers = Some(userAnswers))
        .overrides(
          bind[SubscriptionService].toInstance(mockSubscriptionService),
          bind[CheckYourAnswersHelper].toInstance(mockCheckYourAnswersHelper),
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[Clock].toInstance(clock)
        )
        .build()
  }
}
