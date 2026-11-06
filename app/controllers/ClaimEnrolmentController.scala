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

package controllers

import config.{ClaimEnrolmentInformationProvider, FrontendAppConfig}
import controllers.actions.*
import forms.ClaimEnrolmentFormProvider
import models.requests.DataRequest
import models.{BusinessDetails, UserAnswers}
import pages.PasscodePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{EnrolmentService, RegistrationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.LoggerUtil.*
import views.html.ClaimEnrolmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClaimEnrolmentController @Inject() (
    override val messagesApi: MessagesApi,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    retrieveCtUTR: CtUtrRetrievalAction,
    formProvider: ClaimEnrolmentFormProvider,
    registrationService: RegistrationService,
    enrolmentService: EnrolmentService,
    claimEnrolmentInformationProvider: ClaimEnrolmentInformationProvider,
    appConfig: FrontendAppConfig,
    val controllerComponents: MessagesControllerComponents,
    view: ClaimEnrolmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] =
    (identify() andThen retrieveCtUTR() andThen getData() andThen requireData).async { implicit request =>
      getBusinessDetailsOrErrorMessage(request).map {
        case Left(errorMessage: String)              =>
          logError(s"[ClaimEnrolmentController][onPageLoad] $errorMessage")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Right(businessDetails: BusinessDetails) =>
          Ok(view(form, businessDetails.name))
      }
    }

  def onSubmit(): Action[AnyContent] =
    (identify() andThen retrieveCtUTR() andThen getData() andThen requireData).async { implicit request =>
      claimEnrolmentInformationProvider.claimEnrolmentInformation match {
        case None                            => logAndRedirectOnError(s"Subscription Identifiers not found")
        case Some(claimEnrolmentInformation) =>
          getBusinessDetailsOrErrorMessage(request).flatMap {
            case Left(errorMessage: String)              =>
              logAndRedirectOnError(errorMessage)
            case Right(businessDetails: BusinessDetails) =>
              form
                .bindFromRequest()
                .fold(
                  formWithErrors => Future.successful(BadRequest(view(formWithErrors, businessDetails.name))),
                  value =>
                    if (value) {
                      claimEnrolmentInformation.find(_.safeId == businessDetails.safeId).match {
                        case None            =>
                          logAndRedirectOnError(s"No Claim Enrolment Information found for the given SAFEID")
                        case Some(foundItem) =>
                          val postCode = businessDetails.address.postalCode
                          enrolmentService.enrol(foundItem.carfId, postCode, foundItem.isAbroad).value.flatMap {
                            case Left(_)  =>
                              logAndRedirectOnError("Create enrolment failed")
                            case Right(_) =>
                              logWarn("[ClaimEnrolmentController][onPageLoad] Claim enrolment success!")
                              Future.successful(Redirect(handlePasscodeRouting(request.userAnswers)))
                          }
                      }
                    } else {
                      logAndRedirectOnError("User selected 'No' on page")
                    }
                )
          }
      }
    }

  private def getBusinessDetailsOrErrorMessage(
      request: DataRequest[_]
  )(implicit hc: HeaderCarrier): Future[Either[String, BusinessDetails]] =
    request.utr.map(_.uniqueTaxPayerReference) match {
      case None      =>
        Future.successful(Left("No UTR present in request"))
      case Some(utr) =>
        registrationService.getBusinessWithUtr(request.userAnswers, utr).map {
          case Left(value)            =>
            Left("Error from registration service")
          case Right(businessDetails) =>
            Right(businessDetails)
        }
    }

  private def handlePasscodeRouting(userAnswers: UserAnswers): String =
    if (appConfig.passcodeCheckEnabled)
      s"${appConfig.managementRoutingUrl}/passcode/${userAnswers.get(PasscodePage).getOrElse("")}"
    else
      appConfig.managementRoutingUrl

  private def logAndRedirectOnError(message: String): Future[Result] =
    logError(s"[ClaimEnrolmentController][onPageLoad] $message")
    Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
}
