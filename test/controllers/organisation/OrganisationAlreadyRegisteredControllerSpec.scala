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

package controllers.organisation

import base.SpecBase
import controllers.routes
import models.JourneyType.OrgWithUtr
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.organisation.OrganisationAlreadyRegisteredView

import scala.concurrent.Future

class OrganisationAlreadyRegisteredControllerSpec extends SpecBase {

  val testTaxAndSchemeManagement: String = "https://www.tax.service.gov.uk/tax-and-scheme-management/services"

  val testAeoiEmailAddress: String = "aeoi.enquiries@hmrc.gov.uk"

  "OrganisationAlreadyRegistered Controller" - {

    "must return OK and the correct view for a GET" in {
      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers.copy(journeyType = Some(OrgWithUtr)))).build()
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      running(application) {
        val request =
          FakeRequest(GET, controllers.organisation.routes.OrganisationAlreadyRegisteredController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrganisationAlreadyRegisteredView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(testTaxAndSchemeManagement, testAeoiEmailAddress)(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
