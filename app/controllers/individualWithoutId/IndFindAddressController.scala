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

package controllers.individualWithoutId

import connectors.AddressLookupConnector
import controllers.actions.*
import controllers.routes
import forms.IndFindAddressFormProvider
import models.requests.SearchByPostcodeRequest
import models.responses.AddressResponse
import models.{IndFindAddress, Mode}
import navigation.Navigator
import pages.IndFindAddressPage
import play.api.Logging
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.individualWithoutId.IndFindAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndFindAddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: IndFindAddressFormProvider,
    addressLookupConnector: AddressLookupConnector,
    val controllerComponents: MessagesControllerComponents,
    view: IndFindAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[IndFindAddress] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(IndFindAddressPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      val formReturned = form.bindFromRequest()
      formReturned
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            addressLookupConnector
              .searchByPostcode(SearchByPostcodeRequest(postcode = value.postcode, filter = value.propertyNameOrNumber))
              .flatMap {
                case Nil if value.propertyNameOrNumber.isDefined =>
                  addressLookupConnector
                    .searchByPostcode(SearchByPostcodeRequest(postcode = value.postcode, filter = None))
                    .flatMap {
                      case Nil =>
                        val formError =
                          formReturned.withError(FormError("postcode", List("indFindAddress.error.postcode.notFound")))
                        Future.successful(BadRequest(view(formError, mode)))

                      case addresses =>
                        for {
                          updatedAnswers <- Future.fromTry(request.userAnswers.set(IndFindAddressPage, value))
                          _              <- sessionRepository.set(updatedAnswers)
                        } yield redirectBasedOnAddressCount(addresses, mode)
                    }

                case Nil =>
                  val formError =
                    formReturned.withError(FormError("postcode", List("indFindAddress.error.postcode.notFound")))
                  Future.successful(BadRequest(view(formError, mode)))

                case addresses =>
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(IndFindAddressPage, value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield redirectBasedOnAddressCount(addresses, mode)
                // Redirect(navigator.nextPage(IndFindAddressPage, mode, updatedAnswers))
              }
        )

  }

  private def redirectBasedOnAddressCount(addresses: Seq[AddressResponse], mode: Mode): Result =
    if (addresses.size == 1) {
      Redirect(
        routes.PlaceholderController.onPageLoad(
          s"Must redirect to /register/individual-without-id ${addresses.head.address}"
        )
      )
    } else {
      logger.error(s"choose-address with formatter ${formatAddressResponses(addresses.take(5))}")
      Redirect(routes.PlaceholderController.onPageLoad(s" First address ${addresses.head.address}"))
    }

  private def formatAddressResponses(responses: Seq[AddressResponse]): String =
    s"Total responses = ${responses.length}\n\n\n" +
      responses.zipWithIndex
        .map { case (resp, idx) =>
          val a = resp.address

          s"""
             |#${idx + 1}
             |ID: ${resp.id}
             |Address:
             |  ${a.lines.mkString("\n  ")}
             |Town: ${a.town}
             |Postcode: ${a.postcode}
             |Country: ${a.country.name} (${a.country.code})
             |""".stripMargin.trim
        }
        .mkString("\n\n---\n\n")

}
