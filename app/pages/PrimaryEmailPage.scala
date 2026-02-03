package pages

import pages.QuestionPage
import play.api.libs.json.JsPath

case object PrimaryEmailPage extends QuestionPage[String] {
  override def path: JsPath = JsPath \ "primaryEmail"
}
