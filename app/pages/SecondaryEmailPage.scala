package pages

import pages.QuestionPage
import play.api.libs.json.JsPath

case object SecondaryEmailPage extends QuestionPage[String] {
  override def path: JsPath = JsPath \ "secondaryEmail"
}
