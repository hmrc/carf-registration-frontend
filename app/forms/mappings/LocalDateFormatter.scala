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
import java.time.{LocalDate, Month}
import java.time.format.DateTimeFormatter
import models.DateHelper.formatDateToString
import scala.collection.immutable.Seq
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
    val dayStr      = cleanedData.getOrElse(s"$key.day", "")
    val monthStr    = cleanedData.getOrElse(s"$key.month", "")
    val yearStr     = cleanedData.getOrElse(s"$key.year", "")

    missingFieldErrors(key, dayStr, monthStr, yearStr)
      .orElse(nonNumericFieldErrors(key, dayStr, monthStr, yearStr))
      .orElse(outOfRangeFieldErrors(key, dayStr, monthStr, yearStr))
      .orElse(dateRangeErrors(key, dayStr, monthStr, yearStr))
      .map(Left(_))
      .getOrElse {
        val dayInt   = Try(dayStr.toInt).getOrElse(-1)
        val monthInt = parseMonth(monthStr).getOrElse(-1)
        val yearInt  = Try(yearStr.toInt).getOrElse(-1)
        Right(LocalDate.of(yearInt, monthInt, dayInt))
      }
  }

  private def missingFieldErrors(
      key: String,
      dayStr: String,
      monthStr: String,
      yearStr: String
  ): Option[Seq[FormError]] = {
    val missingFields = fieldKeys.collect {
      case "day" if dayStr.isEmpty     => "day"
      case "month" if monthStr.isEmpty => "month"
      case "year" if yearStr.isEmpty   => "year"
    }
    if (missingFields.isEmpty) None
    else {
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
      Some(errors)
    }
  }

  private def nonNumericFieldErrors(
      key: String,
      dayStr: String,
      monthStr: String,
      yearStr: String
  ): Option[Seq[FormError]] = {
    val dayNonNumeric    = !dayStr.forall(_.isDigit)
    val monthOpt         = parseMonth(monthStr)
    val monthNonNumeric  = monthOpt.isEmpty && !monthStr.forall(_.isDigit)
    val yearNonNumeric   = !yearStr.forall(_.isDigit)
    val nonNumericFields = List(
      "day"   -> dayNonNumeric,
      "month" -> monthNonNumeric,
      "year"  -> yearNonNumeric
    ).collect { case (field, true) => field }
    if (nonNumericFields.nonEmpty)
      Some(nonNumericFields.map(fieldKey => FormError(s"$key.$fieldKey", invalidKey, args)))
    else None
  }

  private def outOfRangeFieldErrors(
      key: String,
      dayStr: String,
      monthStr: String,
      yearStr: String
  ): Option[Seq[FormError]] = {
    val dayInt   = Try(dayStr.toInt).getOrElse(-1)
    val monthOpt = parseMonth(monthStr)
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

    outOfRangeFields match {
      case Nil               => None
      case List(singleField) =>
        Some(Seq(FormError(s"$key.$singleField", notRealDateKey, args)))
      case _                 =>
        Some(fieldKeys.map(fk => FormError(s"$key.$fk", notRealDateKey, args)))
    }
  }

  private def dateRangeErrors(
      key: String,
      dayStr: String,
      monthStr: String,
      yearStr: String
  ): Option[Seq[FormError]] = {
    val dayInt   = Try(dayStr.toInt).getOrElse(-1)
    val monthInt = parseMonth(monthStr).getOrElse(-1)
    val yearInt  = Try(yearStr.toInt).getOrElse(-1)
    Try(LocalDate.of(yearInt, monthInt, dayInt)) match {
      case Success(date) =>
        if (date.isAfter(maxDate)) {
          val displayDate = maxDate.plusDays(1).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
          Some(fieldKeys.map(fk => FormError(s"$key.$fk", futureDateKey, Seq(displayDate) ++ args)))
        } else if (date.isBefore(minDate)) {
          Some(fieldKeys.map(fk => FormError(s"$key.$fk", pastDateKey, args)))
        } else None
      case Failure(_)    =>
        Some(fieldKeys.map(fk => FormError(s"$key.$fk", notRealDateKey, args)))
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}
