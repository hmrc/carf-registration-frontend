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
  private val monthNames              = Seq(
    "january",
    "february",
    "march",
    "april",
    "may",
    "june",
    "july",
    "august",
    "september",
    "october",
    "november",
    "december"
  )
  private val monthShortNames         = monthNames.map(_.take(3))

  private def parseMonth(monthStr: String): Option[Int] = {
    val cleaned = monthStr.trim.replaceAll("[\\s-]", "").toLowerCase
    if (cleaned.forall(_.isDigit)) {
      Try(cleaned.toInt).toOption.filter(m => m >= 1 && m <= 12)
    } else {
      monthNames.indexOf(cleaned) match {
        case idx if idx >= 0 => Some(idx + 1)
        case _               =>
          if (cleaned.length == 3) {
            monthShortNames.indexOf(cleaned) match {
              case idx if idx >= 0 => Some(idx + 1)
              case _               => None
            }
          } else None
      }
    }
  }

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
      val errors = missingFields.sorted match {
        case List("day", "month", "year") =>
          Seq(
            FormError(s"$key.day", allRequiredKey, args),
            FormError(s"$key.month", allRequiredKey, args),
            FormError(s"$key.year", allRequiredKey, args)
          )
        case List("day", "month")         =>
          Seq(
            FormError(s"$key.day", dayAndMonthRequiredKey, args),
            FormError(s"$key.month", dayAndMonthRequiredKey, args)
          )
        case List("day", "year")          =>
          Seq(
            FormError(s"$key.day", dayAndYearRequiredKey, args),
            FormError(s"$key.year", dayAndYearRequiredKey, args)
          )
        case List("month", "year")        =>
          Seq(
            FormError(s"$key.month", monthAndYearRequiredKey, args),
            FormError(s"$key.year", monthAndYearRequiredKey, args)
          )
        case List("day")                  =>
          Seq(FormError(s"$key.day", dayRequiredKey, args))
        case List("month")                =>
          Seq(FormError(s"$key.month", monthRequiredKey, args))
        case List("year")                 =>
          Seq(FormError(s"$key.year", yearRequiredKey, args))
        case _                            =>
          Seq(
            FormError(s"$key.day", allRequiredKey, args),
            FormError(s"$key.month", allRequiredKey, args),
            FormError(s"$key.year", allRequiredKey, args)
          )
      }
      return Left(errors)
    }

    val dayNonNumeric   = !dayStr.forall(_.isDigit)
    val monthOpt        = parseMonth(monthStr)
    val monthNonNumeric = monthOpt.isEmpty && !monthStr.forall(_.isDigit)
    val yearNonNumeric  = !yearStr.forall(_.isDigit)

    val nonNumericFields = List(
      "day"   -> dayNonNumeric,
      "month" -> monthNonNumeric,
      "year"  -> yearNonNumeric
    ).collect { case (field, true) => field }
    if (nonNumericFields.nonEmpty) {
      val errors = nonNumericFields.map { fieldKey =>
        FormError(s"$key.$fieldKey", invalidKey, args)
      }
      return Left(errors)
    }

    val dayInt   = Try(dayStr.toInt).getOrElse(-1)
    val monthInt = monthOpt.getOrElse(-1)
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
          val errors      = fieldKeys.map(fk => FormError(s"$key.$fk", futureDateKey, Seq(displayDate) ++ args))
          return Left(errors)
        }
        if (date.isBefore(minDate)) {
          val errors = fieldKeys.map(fk => FormError(s"$key.$fk", pastDateKey, args))
          return Left(errors)
        }
        Right(date)
      case Failure(_)    =>
        val errors = fieldKeys.map(fk => FormError(s"$key.$fk", notRealDateKey, args))
        Left(errors)
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}
