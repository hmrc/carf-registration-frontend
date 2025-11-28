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

import base.{ControllerMockFixtures, SpecBase}
import cats.implicits.*
import generators.ModelGenerators
import models.error.ApiError
import models.error.ApiError.*
import models.{IndividualRegistrationType, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar.*
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.*
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{RequestHeader, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.RegistrationService
import uk.gov.hmrc.auth.core.AffinityGroup
import viewmodels.govuk.SummaryListFluency
import views.html.CheckYourAnswersView

class CheckYourAnswersControllerSpec
    extends SpecBase
    with ControllerMockFixtures
    with BeforeAndAfterEach
    with TableDrivenPropertyChecks
    with ScalaCheckPropertyChecks
    with ModelGenerators {

  lazy val loadRoute: String = routes.CheckYourAnswersController.onPageLoad().url

  final val mockRegistrationService = mock[RegistrationService]

  val firstContactName  = "first-contact-name"
  val firstContactEmail = "first-contact-email"
  val firstContactPhone = "+44 5021 654 1234"

  override def beforeEach(): Unit = {
    reset(mockRegistrationService)
    super.beforeEach()
  }

  val userAnswers: UserAnswers = emptyUserAnswers
    .withPage(FirstContactEmailPage, "tester@test.com")
    .withPage(FirstContactPhoneNumberPage, "7778889993")

  "Check Your Answers Controller" - {

    "onPageLoad" - {
      "when affinity group is Individual" - {
        "must return OK and the correct view for a GET valid answers for individual with id" in {
          val application = applicationBuilder(userAnswers = Option(userAnswers), AffinityGroup.Individual)
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

            val result = route(application, request).value

            status(result) mustEqual OK

          }
        }

        "redirect to Missing Information when missing some UserAnswers for individual with id" in {
          val application = applicationBuilder(userAnswers = Option(userAnswers), AffinityGroup.Individual)
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
            )
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)

            val result = route(application, request).value

            status(result)              mustEqual SEE_OTHER
            redirectLocation(result).value mustBe routes.InformationMissingController.onPageLoad().url
          }
        }

      }
    }

  }
}
