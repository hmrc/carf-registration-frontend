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
import models.requests.DataRequest
import models.{format, AddressUk, IndFindAddress, Mode}
import navigation.Navigator
import pages.AddressLookupPage
import pages.individualWithoutId.{IndFindAddressAdditionalCallUa, IndFindAddressPage, IndWithoutIdChooseAddressPage, IndWithoutIdSelectedChooseAddressPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CountryListFactory
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

  private case class WithRadiosResult(result: Result, addresses: Seq[AddressUk])

  val form: Form[String] = formProvider()

  private lazy val indWithoutIdAddressControllerRedirect: Mode => Result = mode =>
    Redirect(controllers.individualWithoutId.routes.IndWithoutIdAddressController.onPageLoad(mode))

  private def additionalLine(property: String, postCode: String): String =
    s"We could not find a match for ‘$property’ — showing all results for $postCode instead."

  private def generateHtml(maybeIndFindAddress: Option[IndFindAddress]): Option[String] =
    maybeIndFindAddress.map { indFindAddress =>
      s"""${additionalLine(
          indFindAddress.propertyNameOrNumber.getOrElse(""),
          indFindAddress.postcode
        )}"""
    }

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      lazy val preparedForm: Form[String] = request.userAnswers.get(IndWithoutIdChooseAddressPage).fold(form)(form.fill)

      val WithRadiosResult(result, _) = resultWithRadios(mode) { (radios, maybeIndFindAddress) =>

        val maybeHtml: Option[String] = generateHtml(maybeIndFindAddress)

        Ok(view(preparedForm, mode, radios, maybeHtml))
      }

      Future.successful(result)
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val WithRadiosResult(result, _) = resultWithRadios(mode) { (radios, maybeIndFindAddress) =>

              val maybeHtml: Option[String] = generateHtml(maybeIndFindAddress)
              BadRequest(view(formWithErrors, mode, radios, maybeHtml))

            }
            Future.successful(result)
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
  ): Future[Option[AddressUk]] = Future.fromTry {
    val WithRadiosResult(result, addresses) = resultWithRadios(mode) { (_, _) =>
      Redirect(call = controllers.routes.JourneyRecoveryController.onPageLoad())
    }

    val exception = new Exception("Failed to find address")
    addresses
      .find(_.format(CountryListFactory.ukCountries) == value)
      .fold {
        if (value == noneOfTheseValue) {
          Success(None)
        } else {
          Failure[Option[AddressUk]](exception)
        }
      }(address => Success(Some(address)))
  }

  private def createAddressRadios(addresses: => Seq[AddressUk]): Seq[RadioItem] =
    addresses.map { address =>
      val addressFormatted = address.format(CountryListFactory.ukCountries)
      RadioItem(content = Text(s"$addressFormatted"), value = Some(s"$addressFormatted"))
    }

  private def resultWithRadios(
      mode: Mode
  )(
      result: (Seq[RadioItem], Option[IndFindAddress]) => Result
  )(implicit request: DataRequest[AnyContent]): WithRadiosResult =
    request.userAnswers
      .get(AddressLookupPage)
      .fold {
        WithRadiosResult(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()), Seq.empty)
      } { addresses =>
        if (addresses.isEmpty) {
          WithRadiosResult(indWithoutIdAddressControllerRedirect(mode), Seq.empty)
        } else {
          lazy val radios: Seq[RadioItem] = createAddressRadios(addresses)

          val maybeWithRadiosResult = for {
            indFindAddress     <- request.userAnswers.get(IndFindAddressPage)
            additionalCallMade <- request.userAnswers.get(IndFindAddressAdditionalCallUa)
          } yield WithRadiosResult(
            result = result(
              radios,
              if (additionalCallMade) Some(indFindAddress) else None
            ),
            addresses = addresses
          )

          maybeWithRadiosResult.fold(
            WithRadiosResult(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()), Seq.empty)
          )(identity)
        }
      }
}
