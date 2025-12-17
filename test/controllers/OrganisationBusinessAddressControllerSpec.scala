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
import forms.OrganisationBusinessAddressFormProvider
import models.{Country, NormalMode, OrganisationBusinessAddress, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.OrganisationBusinessAddressPage
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.CountryListFactory
import views.html.orgWithoutId.OrganisationBusinessAddressView

import scala.concurrent.Future

class OrganisationBusinessAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute: Call                             = Call("GET", "/foo")
  lazy val organisationBusinessAddressRoute: String =
    controllers.orgWithoutId.routes.OrganisationBusinessAddressController.onPageLoad(NormalMode).url

  val validAddress: OrganisationBusinessAddress = OrganisationBusinessAddress(
    addressLine1 = "1 Test Street",
    addressLine2 = Some("Testington"),
    townOrCity = "Testtown",
    region = Some("Testregion"),
    postcode = Some("TE1 1ST"),
    country = Country("FR", "France")
  )
  val userAnswers: UserAnswers                  =
    UserAnswers(userAnswersId).set(OrganisationBusinessAddressPage, validAddress).success.value

  def getDependencies(
      application: Application
  ): (OrganisationBusinessAddressFormProvider, Form[OrganisationBusinessAddress], Seq[Country]) = {
    val countryListFactory = application.injector.instanceOf[CountryListFactory]
    val countries          = countryListFactory.countryList.getOrElse(Seq.empty).filterNot(_.code == "GB")
    val formProvider       = new OrganisationBusinessAddressFormProvider()
    val form               = formProvider(countries)
    (formProvider, form, countries)
  }

  "OrganisationBusinessAddress Controller" - {

    "must return OK and the correct view for a GET" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val (_, form, countries) = getDependencies(application)
        val countryListFactory   = application.injector.instanceOf[CountryListFactory]
        val countrySelectItems   = countryListFactory.countrySelectList(form.data, countries)
        val request              = FakeRequest(GET, organisationBusinessAddressRoute)
        val result               = route(application, request).value
        val view                 = application.injector.instanceOf[OrganisationBusinessAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, countrySelectItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {
      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val (_, form, countries) = getDependencies(application)
        val countryListFactory   = application.injector.instanceOf[CountryListFactory]
        val countrySelectItems   = countryListFactory.countrySelectList(form.data, countries)
        val request              = FakeRequest(GET, organisationBusinessAddressRoute)
        val result               = route(application, request).value
        val view                 = application.injector.instanceOf[OrganisationBusinessAddressView]

        status(result)          mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAddress), NormalMode, countrySelectItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
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
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val (_, form, countries) = getDependencies(application)
        val countryListFactory   = application.injector.instanceOf[CountryListFactory]
        val countrySelectItems   = countryListFactory.countrySelectList(form.data, countries)
        val request              =
          FakeRequest(POST, organisationBusinessAddressRoute)
            .withFormUrlEncodedBody(("addressLine1", ""))

        val boundForm = form.bind(Map("addressLine1" -> ""))
        val view      = application.injector.instanceOf[OrganisationBusinessAddressView]
        val result    = route(application, request).value

        status(result)          mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, countrySelectItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return a Bad Request and errors when postcode is empty for a Crown Dependency (Guernsey)" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(POST, organisationBusinessAddressRoute)
          .withFormUrlEncodedBody(
            "addressLine1" -> "1 Test Street",
            "townOrCity"   -> "St Peter Port",
            "country"      -> "GG",
            "postcode"     -> ""
          )
        val result  = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }
    }

    "must return a Bad Request and errors when postcode has an invalid format for a Crown Dependency (Jersey)" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
      running(application) {
        val request = FakeRequest(POST, organisationBusinessAddressRoute)
          .withFormUrlEncodedBody(
            "addressLine1" -> "1 Test Street",
            "townOrCity"   -> "Saint Helier",
            "country"      -> "JE",
            "postcode"     -> "INVALID"
          )
        val result  = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }
    }

    "must redirect to the next page when a valid Crown Dependency address is submitted (Isle of Man)" in {
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[Navigator].toInstance(new FakeNavigator(onwardRoute)))
        .build()

      running(application) {
        val request = FakeRequest(POST, organisationBusinessAddressRoute)
          .withFormUrlEncodedBody(
            "addressLine1" -> "1 Test Street",
            "townOrCity"   -> "Douglas",
            "country"      -> "IM",
            "postcode"     -> "IM1 2EL"
          )
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
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
        val request =
          FakeRequest(POST, organisationBusinessAddressRoute)
            .withFormUrlEncodedBody(("AddressLine1", "value 1"), ("AddressLine2", "value 2"))
        val result  = route(application, request).value
        status(result)                 mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
