package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.OrgYourContactDetailsView

class OrgYourContactDetailsControllerSpec extends SpecBase {

  "OrgYourContactDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.OrgYourContactDetailsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[OrgYourContactDetailsView]
        val expectedContinueUrl: String = routes.PlaceholderController.onPageLoad("Must redirect to /register/contact-name (CARF-178)").url

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedContinueUrl)(request, messages(application)).toString
      }
    }
  }
}
