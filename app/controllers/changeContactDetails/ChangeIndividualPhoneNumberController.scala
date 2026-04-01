package controllers.changeContactDetails

import controllers.actions.*
import forms.individual.IndividualPhoneNumberFormProvider
import models.Mode
import navigation.Navigator
import pages.changeContactDetails.ChangeDetailsIndividualPhoneNumberPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.ChangeIndividualPhoneNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeIndividualPhoneNumberController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        submissionLock: SubmissionLockAction,
                                        requireData: DataRequiredAction,
                                        formProvider: IndividualPhoneNumberFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: ChangeIndividualPhoneNumberView
                                      )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen submissionLock andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(ChangeDetailsIndividualPhoneNumberPage).fold(form)(form.fill)

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify() andThen getData() andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(ChangeDetailsIndividualPhoneNumberPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(ChangeIndividualPhoneNumberPage, mode, updatedAnswers))
      )
  }
}
