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

import controllers.actions.*
import forms.IsThisYourBusinessFormProvider
import models.*
import models.requests.DataRequest
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.RegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswersHelper
import views.html.IsThisYourBusinessView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IsThisYourBusinessController @Inject() (
    override val messagesApi: MessagesApi,
    sessionRepository: SessionRepository,
    navigator: Navigator,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: IsThisYourBusinessFormProvider,
    businessService: RegistrationService,
    val controllerComponents: MessagesControllerComponents,
    view: IsThisYourBusinessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging
    with UserAnswersHelper {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      if (isSoleTrader(request.userAnswers)) {
        logger.info("Handling request as a Sole Trader journey.")
        handleSoleTraderLookup(mode)
      } else {
        logger.info("Handling request as an Organisation (non-Sole Trader) journey.")
        handleOrganisationLookup(mode)
      }
  }

  private def handleSoleTraderLookup(mode: Mode)(implicit request: DataRequest[AnyContent]): Future[Result] =
    request.userAnswers.get(YourUniqueTaxpayerReferencePage) match {
      case Some(utr) =>
        val businessDetailsOpt = if (utr.uniqueTaxPayerReference.startsWith("3")) {
          None
        } else {
          Some(
            BusinessDetails(
              name = "Test Name Sole Trader",
              address = Address("123 Sole Trader Street", None, None, None, Some("ST1 1ST"), "GB")
            )
          )
        }

        businessDetailsOpt match {
          case Some(businessDetails) =>
            val pageDetails = IsThisYourBusinessPageDetails(
              name = businessDetails.name,
              address = businessDetails.address,
              pageAnswer = request.userAnswers.get(IsThisYourBusinessPage).flatMap(_.pageAnswer)
            )
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, pageDetails))
              _              <- sessionRepository.set(updatedAnswers)
            } yield {
              val preparedForm = pageDetails.pageAnswer.fold(form)(form.fill)
              Ok(view(preparedForm, mode, businessDetails))
            }
          case None                  =>
            logger.warn(s"Sole Trader with UTR not found. Redirecting.")
            Future.successful(
              Redirect(
                routes.PlaceholderController.onPageLoad(
                  "Must redirect to /problem/sole-trader-not-identified (CARF-129)"
                )
              )
            )
        }
      case None      =>
        logger.warn("Sole Trader journey: No UTR found in user answers. Redirecting to journey recovery.")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }

  private def handleOrganisationLookup(mode: Mode)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val maybeIndexPage   = request.userAnswers.get(IndexPage)
    val maybeYourUtrPage = request.userAnswers.get(YourUniqueTaxpayerReferencePage)

    (maybeIndexPage, maybeYourUtrPage) match {
      case (Some(autoMatchUtr), _) =>
        handleBusinessLookup(
          businessService.getBusinessWithEnrolmentCtUtr(autoMatchUtr.uniqueTaxPayerReference),
          autoMatchUtr.uniqueTaxPayerReference,
          mode,
          isAutoMatch = true
        )
      case (_, Some(userInputUtr)) =>
        handleBusinessLookup(
          businessService.getBusinessWithUserInput(request.userAnswers),
          userInputUtr.uniqueTaxPayerReference,
          mode,
          isAutoMatch = false
        )
      case (_, _)                  =>
        logger.warn("Organisation journey, No UTR found in user answers. Redirecting to journey recovery.")
        Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>
      request.userAnswers.get(IsThisYourBusinessPage) match {
        case None              =>
          logger.warn("IsThisYourBusinessPage details not found in session on POST. Redirecting to journey recovery.")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        case Some(pageDetails) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                val businessDetails = BusinessDetails(pageDetails.name, pageDetails.address)
                Future.successful(BadRequest(view(formWithErrors, mode, businessDetails)))
              },
              value =>
                for {
                  updatedAnswers <-
                    Future.fromTry(
                      request.userAnswers.set(IsThisYourBusinessPage, pageDetails.copy(pageAnswer = Some(value)))
                    )
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(IsThisYourBusinessPage, mode, updatedAnswers))
            )
      }
  }

  private def handleBusinessLookup(
      lookupFuture: Future[Option[BusinessDetails]],
      utr: String,
      mode: Mode,
      isAutoMatch: Boolean
  )(implicit request: DataRequest[AnyContent]): Future[Result] =
    lookupFuture.flatMap {
      case Some(business) =>
        val existingPageDetails = request.userAnswers.get(IsThisYourBusinessPage)
        val pageDetails         = IsThisYourBusinessPageDetails(
          name = business.name,
          address = business.address,
          pageAnswer = existingPageDetails.flatMap(_.pageAnswer)
        )

        for {
          updatedAnswers <- Future.fromTry(request.userAnswers.set(IsThisYourBusinessPage, pageDetails))
          _              <- sessionRepository.set(updatedAnswers)
        } yield {
          val preparedForm = pageDetails.pageAnswer.fold(form)(form.fill)
          logger.info(s"Business data found and cached for UTR: $utr.")
          Ok(view(preparedForm, mode, business))
        }

      case None =>
        if (isAutoMatch) {
          logger.warn("Auto-match failed for a non-Sole Trader. Redirecting to journey recovery.")
          Future.successful(Redirect(routes.JourneyRecoveryController.onPageLoad()))
        } else {
          logger.warn("Manual entry failed for a non-Sole Trader. Redirecting to business-not-identified.")
          Future.successful(
            Redirect(
              routes.PlaceholderController
                .onPageLoad("Must redirect to /problem/business-not-identified (CARF-147)")
            )
          )
        }
    }
}
