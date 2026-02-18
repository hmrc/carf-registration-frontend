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

import config.Constants.noneOfTheseValue
import controllers.actions.*
import forms.IndWithoutChooseAddressFormProvider
import models.Mode
import models.requests.DataRequest
import models.responses.{format, AddressResponse}
import navigation.Navigator
import pages.AddressLookupPage
import pages.individualWithoutId.{IndWithoutIdChooseAddressPage, IndWithoutIdSelectedChooseAddressPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.IndWithoutChooseAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class IndWithoutChooseAddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: IndWithoutChooseAddressFormProvider,
    val controllerComponents: MessagesControllerComponents,
    view: IndWithoutChooseAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String]                                                         = formProvider()
  private lazy val indWithoutIdAddressControllerRedirect: Mode => Future[Result] = mode =>
    Future.successful(Redirect(controllers.individualWithoutId.routes.IndWithoutIdAddressController.onPageLoad(mode)))

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      lazy val preparedForm: Form[String] = request.userAnswers.get(IndWithoutIdChooseAddressPage).fold(form)(form.fill)

      val (result, _) = resultWithRadios(mode) { radios =>
        Future.successful(Ok(view(preparedForm, mode, radios)))
      }
      result
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val (result, _) = resultWithRadios(mode) { radios =>
              Future.successful(BadRequest(view(formWithErrors, mode, radios)))
            }
            result
          },
          value =>
            for {
              updatedAnswers               <- Future.fromTry(request.userAnswers.set(IndWithoutIdChooseAddressPage, value))
              addressToStoreMaybe          <- findAddressToStore(mode, value)
              updatedAnswersAsAddressMaybe <-
                addressToStoreMaybe.fold(Future.successful(updatedAnswers)) { addressToStore =>
                  Future.fromTry(updatedAnswers.set(IndWithoutIdSelectedChooseAddressPage, addressToStore))
                }
              _                            <- sessionRepository.set(updatedAnswersAsAddressMaybe)
            } yield Redirect(navigator.nextPage(IndWithoutIdChooseAddressPage, mode, updatedAnswersAsAddressMaybe))
        )
  }

  private def findAddressToStore(mode: Mode, value: String)(implicit
      request: DataRequest[AnyContent]
  ): Future[Option[AddressResponse]] = Future.fromTry {

    val (_, addresses) = resultWithRadios(mode) { _ =>
      Future.successful(Redirect(call = controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

    val exception = new Exception("Failed to find address")
    addresses
      .find(_.address.format == value)
      .fold {
        if (value == noneOfTheseValue) {
          Success(None)
        } else {
          Failure[Option[AddressResponse]](exception)
        }
      }(address => Success(Some(address)))
  }

  private def createAddressRadios(addressResponses: => Seq[AddressResponse]): Seq[RadioItem] =
    addressResponses.map { addressResponse =>
      val addressFormatted = addressResponse.address.format
      RadioItem(content = Text(s"$addressFormatted"), value = Some(s"$addressFormatted"))
    }

  private def resultWithRadios(
      mode: Mode
  )(
      result: Seq[RadioItem] => Future[Result]
  )(implicit request: DataRequest[AnyContent]): (Future[Result], Seq[AddressResponse]) =
    request.userAnswers
      .get(AddressLookupPage)
      .fold {
        (Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())), Seq.empty)
      } { addressResponses =>
        lazy val radios: Seq[RadioItem] = createAddressRadios(addressResponses)

        if (addressResponses.isEmpty) {
          (indWithoutIdAddressControllerRedirect(mode), Seq.empty)
        } else {
          (result(radios), addressResponses)
        }
      }
}
