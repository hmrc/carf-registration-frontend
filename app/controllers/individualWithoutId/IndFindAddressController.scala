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

import controllers.actions.*
import forms.individualWithoutId.IndFindAddressFormProvider
import models.requests.DataRequest
import models.{AddressAndUPRN, AddressUk, IndFindAddress, Mode, NormalMode}
import navigation.Navigator
import pages.individualWithoutId.*
import play.api.data.{Form, FormError}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AddressLookupService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.LoggerUtil.*
import views.html.individualWithoutId.IndFindAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IndFindAddressController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    submissionLock: SubmissionLockAction,
    requireData: DataRequiredAction,
    formProvider: IndFindAddressFormProvider,
    addressLookupService: AddressLookupService,
    val controllerComponents: MessagesControllerComponents,
    view: IndFindAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[IndFindAddress] = formProvider()

  lazy val manualLink: Mode => String =
    mode => controllers.individualWithoutId.routes.IndWithoutIdAddressController.onPageLoad(mode).url

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify() andThen getData() andThen submissionLock andThen requireData).async { implicit request =>

      lazy val preparedForm = request.userAnswers.get(IndFindAddressPage).fold(form)(form.fill)

      for {
        userAnswersNoPrePop <- {
          mode match {
            case NormalMode => Future.fromTry(request.userAnswers.remove(IndWithoutIdAddressPagePrePop))
            case _          => Future.successful(request.userAnswers)
          }
        }
        _                   <- sessionRepository.set(userAnswersNoPrePop)
      } yield Ok(view(preparedForm, mode, manualLink(mode)))
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      val formReturned = form.bindFromRequest()
      formReturned
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, manualLink(mode)))),
          value =>
            addressLookupService
              .postcodeSearch(value.postcode, value.propertyNameOrNumber)
              .flatMap {
                case Left(error)                                    =>
                  logError(s"[IndFindAddressController][onSubmit] Address lookup service failed: $error")
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                case Right((Nil, _))                                =>
                  val formError =
                    formReturned.withError(FormError("postcode", List("indFindAddress.error.postcode.notFound")))
                  Future.successful(BadRequest(view(formError, mode, manualLink(mode))))
                case Right((addressesAndUPRNs, additionalCallMade)) =>
                  for {
                    updatedAnswers <-
                      if (addressesAndUPRNs.length == 1) { saveSingleAddress(value, addressesAndUPRNs.head) }
                      else { saveMultipleAddresses(value, addressesAndUPRNs, additionalCallMade) }
                  } yield Redirect(navigator.nextPage(IndFindAddressPage, mode, updatedAnswers))
              }
        )
  }

  private def saveMultipleAddresses(
      indFindAddress: IndFindAddress,
      addressesAndUPRNs: Seq[AddressAndUPRN],
      additionalCallMade: Boolean
  )(implicit request: DataRequest[AnyContent]) =
    for {
      uaWithSingleAddressDataCleared <-
        Future.fromTry(request.userAnswers.remove(List(AddressUPRNUserAnswers, IndWithoutIdAddressPagePrePop)))
      uaWithPageAnswer               <-
        Future.fromTry(uaWithSingleAddressDataCleared.set(IndFindAddressPage, indFindAddress))
      uaWithAddresses                <-
        Future.fromTry(uaWithPageAnswer.set(AddressLookupPage, addressesAndUPRNs))
      uaWithAdditionalCallFlag       <-
        Future.fromTry(uaWithAddresses.set(IndFindAddressAdditionalCallUa, additionalCallMade))
      _                              <- sessionRepository.set(uaWithAdditionalCallFlag)
    } yield uaWithAdditionalCallFlag

  private def saveSingleAddress(
      indFindAddress: IndFindAddress,
      addressAndUPRN: AddressAndUPRN
  )(implicit request: DataRequest[AnyContent]) =
    for {
      uaWithMultipleAddressDataCleared <-
        Future.fromTry(request.userAnswers.remove(List(IndFindAddressAdditionalCallUa, AddressLookupPage)))
      uaWithPageAnswer                 <-
        Future.fromTry(uaWithMultipleAddressDataCleared.set(IndFindAddressPage, indFindAddress))
      uaWithPrePop                     <-
        Future.fromTry(uaWithPageAnswer.set(IndWithoutIdAddressPagePrePop, addressAndUPRN.address))
      uaWithUprn                       <-
        Future.fromTry(uaWithPrePop.set(AddressUPRNUserAnswers, addressAndUPRN.UPRN))
      _                                <- sessionRepository.set(uaWithUprn)
    } yield uaWithUprn

}
