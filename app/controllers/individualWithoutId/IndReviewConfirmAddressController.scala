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

package controllers.individualWithoutId

import connectors.AddressLookupConnector
import controllers.actions.*
import controllers.routes
//import forms.individualWithoutId.IndReviewConfirmAddressFormProvider
import models.Mode
import models.responses.{AddressRecord, AddressResponse, CountryRecord}
import navigation.Navigator
import pages.AddressLookupPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individualWithoutId.IndReviewConfirmAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndReviewConfirmAddressController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    val controllerComponents: MessagesControllerComponents,
    view: IndReviewConfirmAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      val nextPageLink: String    = controllers.individual.routes.IndividualEmailController.onPageLoad(mode).url
      val editAddressLink: String =
        controllers.routes.PlaceholderController
          .onPageLoad("Must redirect to /register/individual-without-id/address")
          .url

      val value: Seq[AddressResponse] = Seq(
        AddressResponse(
          id = "GB790091234501",
          address = AddressRecord(
            List("1 Test Street"),
            "Testtown",
            "BB00 0BB",
            CountryRecord("GB", "United Kingdom")
          )
        )
      )

      request.userAnswers.get(AddressLookupPage) match {
        case Some(value) =>
          Future.successful(Ok(view(value.head, mode, editAddressLink, nextPageLink)))
        case None        =>
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
      }
  }

}
