package pages

import models.SubscriptionId
import pages.QuestionPage
import play.api.libs.json.JsPath

case object SubscriptionIdPage extends QuestionPage[SubscriptionId] {
  override def path: JsPath = JsPath \ "subscriptionId"
}
