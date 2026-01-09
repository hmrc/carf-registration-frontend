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

import java.time.{LocalDate, Month}
import java.time.format.DateTimeFormatter
import play.api.data.FormError
import play.api.data.format.Formatter
import scala.util.{Failure, Success, Try}

private[mappings] class LocalDateFormatter(
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
) extends Formatter[LocalDate]
    with Formatters {

  private val fieldKeys: List[String] = List("day", "month", "year")
  private val monthFormatter          = new MonthFormatter(invalidKey, args)

  private case class Field(key: String, value: String, isMissing: Boolean, isInvalid: Boolean)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val cleanedData = data.map { case (k, v) => k -> v.replaceAll("[\\s-]", "") }

    val fields = fieldKeys.map { fieldKey =>
      val value     = cleanedData.get(s"$key.$fieldKey").getOrElse("")
      val isMissing = value.isEmpty
      val isInvalid =
        !isMissing && !value.forall(_.isDigit) && monthFormatter.bind(s"$key.$fieldKey", cleanedData).isLeft
      Field(fieldKey, value, isMissing, isInvalid)
    }

    handleMissing(key, fields)
      .orElse(handleInvalid(key, fields))
      .orElse(handleRealDate(key, fields, cleanedData))
      .getOrElse(constructDate(key, fields, cleanedData))
  }

  private def handleMissing(key: String, fields: Seq[Field]): Option[Either[Seq[FormError], LocalDate]] = {
    val missingFields = fields.filter(_.isMissing).map(_.key)
    if (missingFields.isEmpty) {
      None
    } else {
      val message   = missingFields.sorted match {
        case "day" :: "month" :: "year" :: Nil => allRequiredKey
        case "day" :: "month" :: Nil           => dayAndMonthRequiredKey
        case "day" :: "year" :: Nil            => dayAndYearRequiredKey
        case "month" :: "year" :: Nil          => monthAndYearRequiredKey
        case "day" :: Nil                      => dayRequiredKey
        case "month" :: Nil                    => monthRequiredKey
        case "year" :: Nil                     => yearRequiredKey
        case _                                 => allRequiredKey
      }
      val errorArgs = missingFields.map(field => s"date.error.$field")
      Some(Left(Seq(FormError(key, message, errorArgs ++ args))))
    }
  }

  private def handleInvalid(key: String, fields: Seq[Field]): Option[Either[Seq[FormError], LocalDate]] = {
    val invalidFields = fields.filter(_.isInvalid).map(_.key)
    if (invalidFields.isEmpty) {
      None
    } else {
      val errorArgs =
        if (invalidFields.size > 1) fieldKeys.map(f => s"date.error.$f") else invalidFields.map(f => s"date.error.$f")
      Some(Left(Seq(FormError(key, invalidKey, errorArgs ++ args))))
    }
  }

  private def handleRealDate(
      key: String,
      fields: Seq[Field],
      data: Map[String, String]
  ): Option[Either[Seq[FormError], LocalDate]] = {
    val day   = Try(fields.find(_.key == "day").get.value.toInt).toOption
    val month = Try(monthFormatter.bind(s"$key.month", data).getOrElse(0)).toOption
    val year  = Try(fields.find(_.key == "year").get.value.toInt).toOption

    (day, month, year) match {
      case (Some(d), Some(m), Some(y)) =>
        Try(LocalDate.of(y, m, d)) match {
          case Success(date) =>
            if (date.isAfter(maxDate)) {
              val displayDate = maxDate.plusDays(1).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
              val errorArgs   = Seq(displayDate) ++ fieldKeys.map(f => s"date.error.$f") ++ args
              Some(Left(Seq(FormError(key, futureDateKey, errorArgs))))
            } else if (date.isBefore(minDate)) {
              Some(Left(Seq(FormError(key, pastDateKey, fieldKeys.map(f => s"date.error.$f") ++ args))))
            } else {
              None
            }
          case Failure(_)    =>
            val invalidFields = Seq(
              if (d < 1 || d > 31) Some("day") else None,
              if (m < 1 || m > 12) Some("month") else None,
              if (y < 1000 || y > 9999) Some("year") else None
            ).flatten

            val errorArgs =
              if (invalidFields.nonEmpty) invalidFields.map(f => s"date.error.$f")
              else fieldKeys.map(f => s"date.error.$f")
            Some(Left(Seq(FormError(key, notRealDateKey, errorArgs ++ args))))
        }
      case _                           =>
        Some(Left(Seq(FormError(key, invalidKey, fieldKeys.map(f => s"date.error.$f") ++ args))))
    }
  }

  private def constructDate(
      key: String,
      fields: Seq[Field],
      data: Map[String, String]
  ): Either[Seq[FormError], LocalDate] = {
    val day   = fields.find(_.key == "day").get.value.toInt
    val month = monthFormatter.bind(s"$key.month", data).getOrElse(0)
    val year  = fields.find(_.key == "year").get.value.toInt
    Right(LocalDate.of(year, month, day))
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )

  private class MonthFormatter(invalidKey: String, args: Seq[String]) extends Formatter[Int] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = {
      val monthStr = data.get(key).getOrElse("").trim.toUpperCase
      if (monthStr.forall(_.isDigit)) {
        Try(monthStr.toInt).toEither.left.map(_ => Seq(FormError(key, invalidKey, args)))
      } else {
        Month
          .values()
          .find(m => m.toString == monthStr || m.toString.take(3) == monthStr)
          .map(m => Right(m.getValue))
          .getOrElse(Left(Seq(FormError(key, invalidKey, args))))
      }
    }
    override def unbind(key: String, value: Int): Map[String, String]                      = Map(key -> value.toString)
  }
}
