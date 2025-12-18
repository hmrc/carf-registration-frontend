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
import controllers.routes
import forms.orgWithoutId.OrganisationBusinessAddressFormProvider
import models.{Country, NormalMode, OrganisationBusinessAddress, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import pages.orgWithoutId.OrganisationBusinessAddressPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import utils.CountryListFactory
import views.html.orgWithoutId.OrganisationBusinessAddressView

import scala.concurrent.Future

class OrganisationBusinessAddressControllerSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  def onwardRoute: Call = Call("GET", "/foo")

  val france: Country             = Country("FR", "France")
  val jersey: Country             = Country("JE", "Jersey")
  val mockCountries: Seq[Country] = Seq(france, jersey)

  val formProvider = new OrganisationBusinessAddressFormProvider()
  val form         = formProvider(mockCountries.filterNot(_.code == "GB"))

  lazy val organisationBusinessAddressRoute: String =
    controllers.orgWithoutId.routes.OrganisationBusinessAddressController.onPageLoad(NormalMode).url

  val validAddress: OrganisationBusinessAddress = OrganisationBusinessAddress(
    addressLine1 = "1 Test Street",
    addressLine2 = Some("Testington"),
    townOrCity = "Testtown",
    region = Some("Testregion"),
    postcode = Some("TE1 1ST"),
    country = france
  )

  val userAnswers: UserAnswers =
    UserAnswers(userAnswersId).set(OrganisationBusinessAddressPage, validAddress).success.value

  val mockCountryListFactory: CountryListFactory = mock[CountryListFactory]
  val mockSessionRepository: SessionRepository   = mock[SessionRepository]

  override def beforeEach(): Unit = {
    reset(mockCountryListFactory, mockSessionRepository)
    super.beforeEach()
  }

  "OrganisationBusinessAddress Controller" - {

    "must return OK and the correct view for a GET" in {
      when(mockCountryListFactory.countryList).thenReturn(Some(mockCountries))
      when(mockCountryListFactory.countrySelectList(any(), any())).thenReturn(Seq.empty)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request = FakeRequest(GET, organisationBusinessAddressRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[OrganisationBusinessAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, Seq.empty)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      when(mockCountryListFactory.countryList).thenReturn(Some(mockCountries))
      when(mockCountryListFactory.countrySelectList(any(), any())).thenReturn(Seq.empty)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request = FakeRequest(GET, organisationBusinessAddressRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[OrganisationBusinessAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAddress), NormalMode, Seq.empty)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockCountryListFactory.countryList).thenReturn(Some(mockCountries))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[CountryListFactory].toInstance(mockCountryListFactory)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, organisationBusinessAddressRoute)
            .withFormUrlEncodedBody(
              "addressLine1" -> "1 Test Street",
              "townOrCity"   -> "Paris",
              "country"      -> "FR"
            )

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      when(mockCountryListFactory.countryList).thenReturn(Some(mockCountries))
      when(mockCountryListFactory.countrySelectList(any(), any())).thenReturn(Seq.empty)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, organisationBusinessAddressRoute)
            .withFormUrlEncodedBody(("addressLine1", ""))

        val boundForm = form.bind(Map("addressLine1" -> ""))
        val view      = application.injector.instanceOf[OrganisationBusinessAddressView]
        val result    = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, Seq.empty)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if country list is not available" in {
      when(mockCountryListFactory.countryList).thenReturn(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request = FakeRequest(GET, organisationBusinessAddressRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if country list is not available" in {
      when(mockCountryListFactory.countryList).thenReturn(None)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[CountryListFactory].toInstance(mockCountryListFactory))
        .build()

      running(application) {
        val request = FakeRequest(POST, organisationBusinessAddressRoute)
          .withFormUrlEncodedBody(("addressLine1", "some value"))

        val result = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, organisationBusinessAddressRoute)
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, organisationBusinessAddressRoute)
          .withFormUrlEncodedBody(("addressLine1", "some value"))
        val result  = route(application, request).value

        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
