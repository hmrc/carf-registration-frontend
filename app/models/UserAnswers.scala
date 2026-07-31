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

package models

import models.CryptoType.CryptoT
import models.crypto.{SensitiveJsObject, SensitiveResponse, SensitiveSafeId, SensitiveSubscriptionId}
import models.responses.DisplaySubscriptionResponse
import play.api.libs.functional.syntax.*
import play.api.libs.json.*
import queries.{Gettable, Settable}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant
import scala.util.{Failure, Success, Try}

final case class UserAnswers(
    id: String,
    journeyType: Option[JourneyType] = None,
    changeIsIndividualRegType: Option[Boolean] = None,
    isCtAutoMatched: Boolean = false,
    safeId: Option[SafeId] = None,
    hasValidMatch: Boolean = false,
    subscriptionId: Option[SubscriptionId] = None,
    displaySubscriptionResponse: Option[DisplaySubscriptionResponse] = None,
    data: JsObject = Json.obj(),
    lastUpdated: Instant = Instant.now
) {

  def get[A](page: Gettable[A])(implicit rds: Reads[A]): Option[A] =
    Reads.optionNoError(Reads.at(page.path)).reads(data).getOrElse(None)

  def set[A](page: Settable[A] & Gettable[A], newValue: A)(implicit
      writes: Writes[A],
      rds: Reads[A]
  ): Try[UserAnswers] = {

    lazy val hasValueChanged = !get(page).contains(newValue)

    val updatedData = data.setObject(page.path, Json.toJson(newValue)) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(errors)       =>
        Failure(JsResultException(errors))
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      page.cleanup(newValue, updatedAnswers, hasValueChanged)
    }
  }

  def remove[A](page: Settable[A]): Try[UserAnswers] = {

    val updatedData = data.removeObject(page.path) match {
      case JsSuccess(jsValue, _) =>
        Success(jsValue)
      case JsError(_)            =>
        Success(data)
    }

    updatedData.flatMap { d =>
      val updatedAnswers = copy(data = d)
      Success(updatedAnswers)
    }
  }

  def remove(pages: List[Settable[_]]): Try[UserAnswers] =
    pages.foldLeft(Try(this)) { (oldAnswerList, page) =>
      oldAnswerList.flatMap(_.remove(page))
    }

  def clearMatchFlagAndSafeId: UserAnswers =
    this.copy(safeId = None, hasValidMatch = false)
}

object UserAnswers {

  def mongoFormat(encryptionEnabled: Boolean)(implicit crypto: CryptoT): OFormat[UserAnswers] = {
    implicit val sensitiveSafeIdFormat: Format[SensitiveSafeId] =
      if (encryptionEnabled) {
        JsonEncryption.sensitiveEncrypterDecrypter(SensitiveSafeId.apply)
      } else {
        Json.format[SensitiveSafeId]
      }

    implicit val sensitiveJsObjectFormat: Format[SensitiveJsObject] =
      if (encryptionEnabled) {
        JsonEncryption.sensitiveEncrypterDecrypter(SensitiveJsObject.apply)
      } else {
        Json.format[SensitiveJsObject]
      }

    implicit val sensitiveSubscriptionIdFormat: Format[SensitiveSubscriptionId] =
      if (encryptionEnabled) {
        JsonEncryption.sensitiveEncrypterDecrypter(SensitiveSubscriptionId.apply)
      } else {
        Json.format[SensitiveSubscriptionId]
      }

    implicit val sensitiveResponseFormat: Format[SensitiveResponse] =
      if (encryptionEnabled) {
        JsonEncryption.sensitiveEncrypterDecrypter(SensitiveResponse.apply)
      } else {
        Json.format[SensitiveResponse]
      }

    (
      (__ \ "_id").format[String] and
        (__ \ "journeyType").formatNullable[JourneyType] and
        (__ \ "changeIsIndividualRegType").formatNullable[Boolean] and
        (__ \ "isCtAutoMatched").format[Boolean] and
        (__ \ "safeId").formatNullable[SensitiveSafeId] and
        (__ \ "hasValidMatch").format[Boolean] and
        (__ \ "subscriptionId").formatNullable[SensitiveSubscriptionId] and
        (__ \ "displaySubscriptionResponse").formatNullable[SensitiveResponse] and
        (__ \ "data").format[SensitiveJsObject] and
        (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat)
    )(
      (
          id,
          journeyType,
          changeIsIndividualRegType,
          isCtAutoMatched,
          safeId,
          hasValidMatch,
          subscriptionId,
          displaySubscriptionResponse,
          data,
          lastUpdated
      ) =>
        UserAnswers(
          id,
          journeyType,
          changeIsIndividualRegType,
          isCtAutoMatched,
          safeId.map(_.decryptedValue),
          hasValidMatch,
          subscriptionId.map(_.decryptedValue),
          displaySubscriptionResponse.map(_.decryptedValue),
          data.decryptedValue,
          lastUpdated
        ),
      ua =>
        (
          ua.id,
          ua.journeyType,
          ua.changeIsIndividualRegType,
          ua.isCtAutoMatched,
          ua.safeId.map(SensitiveSafeId(_)),
          ua.hasValidMatch,
          ua.subscriptionId.map(SensitiveSubscriptionId(_)),
          ua.displaySubscriptionResponse.map(SensitiveResponse(_)),
          SensitiveJsObject(ua.data),
          ua.lastUpdated
        )
    )
  }

}
