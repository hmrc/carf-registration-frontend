package pages
import pages.QuestionPage
import play.api.libs.json.JsPath

case object SubmissionSucceededPage extends QuestionPage[Boolean] {
  override def path: JsPath = JsPath \ "submissionSucceeded"
}