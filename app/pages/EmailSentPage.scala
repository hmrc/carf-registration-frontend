package pages

import pages.QuestionPage
import play.api.libs.json.JsPath

case object EmailSentPage extends QuestionPage[Boolean] {
  override def path: JsPath = JsPath \ "emailSent"
}
