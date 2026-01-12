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

package forms.mappings

import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.i18n.Messages
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

class LocalDateFormatter(
    invalidKey: String,
    notRealDateKey: String,
    allRequiredKey: String,
    dayRequiredKey: String,
    monthRequiredKey: String,
    yearRequiredKey: String,
    dayAndMonthRequiredKey: String,
    dayAndYearRequiredKey: String,
    monthAndYearRequiredKey: String,
    futureDateKey: String,
    pastDateKey: String,
    maxDate: LocalDate,
    minDate: LocalDate,
    args: Seq[String] = Seq.empty
)(implicit messages: Messages)
    extends Formatter[LocalDate] {

  private val fieldKeys: List[String] = List("day", "month", "year")

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
    val cleanedData = data.map { case (k, v) => k -> v.replaceAll("[\\s-]", "") }

    val dayStr   = cleanedData.get(s"$key.day").getOrElse("")
    val monthStr = cleanedData.get(s"$key.month").getOrElse("")
    val yearStr  = cleanedData.get(s"$key.year").getOrElse("")

    val missingFields = fieldKeys.filter {
      case "day"   => dayStr.isEmpty
      case "month" => monthStr.isEmpty
      case "year"  => yearStr.isEmpty
    }
    if (missingFields.nonEmpty) {
      val errors = missingFields.map { fieldKey =>
        FormError(
          s"$key.$fieldKey",
          fieldKey match {
            case "day"   => dayRequiredKey
            case "month" => monthRequiredKey
            case "year"  => yearRequiredKey
          },
          args
        )
      }
      return Left(errors)
    }

    val nonNumericFields = fieldKeys.filter {
      case "day"   => !dayStr.forall(_.isDigit)
      case "month" => !monthStr.forall(_.isDigit)
      case "year"  => !yearStr.forall(_.isDigit)
    }
    if (nonNumericFields.nonEmpty) {
      val errors = nonNumericFields.map { fieldKey =>
        FormError(s"$key.$fieldKey", invalidKey, args)
      }
      return Left(errors)
    }

    val dayInt   = Try(dayStr.toInt).getOrElse(-1)
    val monthInt = Try(monthStr.toInt).getOrElse(-1)
    val yearInt  = Try(yearStr.toInt).getOrElse(-1)

    val dayOutOfRange   = !(1 to 31).contains(dayInt)
    val monthOutOfRange = !(1 to 12).contains(monthInt)
    val yearOutOfRange  = !(1000 to 9999).contains(yearInt)

    val outOfRangeFields = List(
      "day"   -> dayOutOfRange,
      "month" -> monthOutOfRange,
      "year"  -> yearOutOfRange
    ).collect { case (field, true) => field }

    if (outOfRangeFields.size == 1) {
      val fk = outOfRangeFields.head
      return Left(Seq(FormError(s"$key.$fk", notRealDateKey, args)))
    } else if (outOfRangeFields.size > 1) {
      val errors = outOfRangeFields.map(fk => FormError(s"$key.$fk", notRealDateKey, args))
      return Left(errors)
    }

    Try(LocalDate.of(yearInt, monthInt, dayInt)) match {
      case Success(date) =>
        if (date.isAfter(maxDate)) {
          val displayDate = maxDate.plusDays(1).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
          return Left(Seq(FormError(key, futureDateKey, Seq(displayDate) ++ args)))
        }
        if (date.isBefore(minDate)) {
          return Left(Seq(FormError(key, pastDateKey, args)))
        }
        Right(date)
      case Failure(_)    =>
        Left(fieldKeys.map(fk => FormError(s"$key.$fk", notRealDateKey, args)))
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}
