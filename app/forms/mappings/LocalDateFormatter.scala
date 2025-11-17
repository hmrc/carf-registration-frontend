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
import models.DateHelper.formatDateToString
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.i18n.Messages

import scala.collection.immutable.Seq
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
)(implicit messages: Messages)
    extends Formatter[LocalDate]
    with Formatters {
  private val fieldKeys: List[String] = List("day", "month", "year")
  private val monthFormatter          = new MonthFormatter(invalidKey, args)

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
    val cleanedData = data.map { case (k, v) => k -> v.replaceAll("[\\s-]", "") }
    val boundFields = bindFields(key, cleanedData)
    val errors      = collectErrors(key, cleanedData, boundFields)

    if (errors.nonEmpty) {
      Left(Seq(buildFinalError(key, errors)))
    } else {
      for {
        year  <- boundFields.year
        month <- boundFields.month
        day   <- boundFields.day
      } yield LocalDate.of(year, month, day)
    }
  }

  private case class BoundDateFields(
      day: Either[Seq[FormError], Int],
      month: Either[Seq[FormError], Int],
      year: Either[Seq[FormError], Int]
  )

  private def bindFields(key: String, data: Map[String, String]): BoundDateFields = {
    val intFmttr    = intFormatter(requiredKey = invalidKey, wholeNumberKey = invalidKey, nonNumericKey = invalidKey, args)
    val dayResult   = intFmttr.bind(s"$key.day", data)
    val monthResult = intFmttr.bind(s"$key.month", data).left.flatMap(_ => monthFormatter.bind(s"$key.month", data))
    val yearResult  = intFmttr.bind(s"$key.year", data)

    BoundDateFields(dayResult, monthResult, yearResult)
  }

  private def collectErrors(key: String, cleanedData: Map[String, String], fields: BoundDateFields): Seq[FormError] = {
    val missingFieldErrors = findMissingFieldErrors(key, cleanedData)
    val validationErrors   = findValidationErrors(fields)
    val realDateError      = findRealDateError(key, fields)

    missingFieldErrors ++ validationErrors ++ realDateError
  }

  private def buildFinalError(key: String, allErrors: Seq[FormError]): FormError = {
    val primaryError        = selectPrimaryError(allErrors)
    val primaryMessageArgs  = primaryError.args.filterNot(isHighlightingArg)
    val allHighlightingArgs = allErrors.flatMap(_.args).distinct.filter(isHighlightingArg)
    FormError(key, primaryError.message, primaryMessageArgs ++ allHighlightingArgs)
  }

  private def findMissingFieldErrors(key: String, cleanedData: Map[String, String]): Seq[FormError] = {
    val missing = fieldKeys.filter(field => cleanedData.get(s"$key.$field").forall(_.isEmpty))
    if (missing.isEmpty) Seq.empty else Seq(handleMissingFields(key, missing))
  }

  private def findValidationErrors(fields: BoundDateFields): Seq[FormError] =
    fields.day.left.toSeq.flatten.map(_.copy(invalidKey, args = Seq("date.error.day"))) ++
      fields.month.left.toSeq.flatten.map(_.copy(invalidKey, args = Seq("date.error.month"))) ++
      fields.year.left.toSeq.flatten.map(_.copy(invalidKey, args = Seq("date.error.year")))

  private def findRealDateError(key: String, fields: BoundDateFields): Seq[FormError] = {
    val maybeDay   = fields.day.toOption
    val maybeMonth = fields.month.toOption
    val maybeYear  = fields.year.toOption

    (maybeDay, maybeMonth, maybeYear) match {
      case (Some(day), Some(month), Some(year))                 =>
        validateRealDate(key, day, month, year)
      case (Some(day), Some(month), None)                       =>
        if (!isPotentiallyValidDayMonth(day, month))
          Seq(FormError(key, notRealDateKey, Seq("date.error.day", "date.error.month") ++ args))
        else Seq.empty
      case (Some(day), None, Some(year))                        =>
        if (day < 1 || day > 31)
          Seq(FormError(key, notRealDateKey, Seq("date.error.day") ++ args))
        else Seq.empty
      case (None, Some(month), Some(year))                      =>
        if (month < 1 || month > 12)
          Seq(FormError(key, notRealDateKey, Seq("date.error.month") ++ args))
        else Seq.empty
      case (Some(day), None, None) if day < 1 || day > 31       =>
        Seq(FormError(key, notRealDateKey, Seq("date.error.day") ++ args))
      case (None, Some(month), None) if month < 1 || month > 12 =>
        Seq(FormError(key, notRealDateKey, Seq("date.error.month") ++ args))
      case _                                                    =>
        Seq.empty
    }
  }

  private def validateRealDate(key: String, day: Int, month: Int, year: Int): Seq[FormError] =
    Try(LocalDate.of(year, month, day)) match {
      case Failure(_)                               =>
        val invalidFields =
          Seq(
            if (day < 1 || day > 31) Some("date.error.day") else None,
            if (month < 1 || month > 12) Some("date.error.month") else None,
            if (year < minDate.getYear || year > maxDate.getYear + 1000) Some("date.error.year") else None
          ).flatten match {
            case Nil => Seq("date.error.day", "date.error.month", "date.error.year")
            case s   => s
          }
        Seq(FormError(key, notRealDateKey, invalidFields ++ args))
      case Success(date) if !date.isBefore(maxDate) =>
        Seq(
          FormError(
            key,
            futureDateKey,
            Seq(formatDateToString(maxDate)) ++
              Seq("date.error.day", "date.error.month", "date.error.year") ++ args
          )
        )
      case Success(date) if date.isBefore(minDate)  =>
        Seq(
          FormError(
            key,
            pastDateKey,
            Seq("date.error.day", "date.error.month", "date.error.year") ++ args
          )
        )
      case _                                        =>
        Seq.empty
    }

  private def isPotentiallyValidDayMonth(day: Int, month: Int): Boolean =
    month >= 1 && month <= 12 && day >= 1 && day <= 31

  private def selectPrimaryError(allErrors: Seq[FormError]): FormError = {
    def findErrorForField(errors: Seq[FormError], field: String): Option[FormError] =
      errors.find(_.args.contains(s"date.error.$field"))

    val categoryAErrors =
      allErrors.filter(e => e.message != notRealDateKey && e.message != futureDateKey && e.message != pastDateKey)

    val realDate = allErrors.find(_.message == notRealDateKey)
    val range    = allErrors.find(e => e.message == futureDateKey || e.message == pastDateKey)

    findErrorForField(categoryAErrors, "day")
      .orElse(findErrorForField(categoryAErrors, "month"))
      .orElse(findErrorForField(categoryAErrors, "year"))
      .orElse(categoryAErrors.headOption)
      .orElse(realDate)
      .orElse(range)
      .getOrElse(allErrors.head)
  }

  private def isHighlightingArg(arg: Any): Boolean = arg match {
    case s: String => s.startsWith("date.error.")
    case _         => false
  }

  private def handleMissingFields(key: String, missingFields: List[String]): FormError = {
    val errorArgs = missingFields.map(field => s"date.error.$field")
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
    FormError(key, message, errorArgs ++ args)
  }

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] =
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        Right(date)
      case Failure(_)    =>
        Left(Seq(FormError(key, notRealDateKey, args)))
    }

  private def getErrorArgs(day: Int, month: Int): Seq[String] = {
    val isDayError   = if (day < 1 || day > 31) true else false
    val isMonthError = if (month < 1 || month > 12) true else false

    (isDayError, isMonthError) match {
      case (true, false) => Seq("day")
      case (false, true) => Seq("month")
      case (_, _)        => Seq("day", "month", "year")
    }
  }

  private def formatDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] =
    toDate(key, day, month, year).flatMap { date =>
      if (date.isAfter(maxDate)) {
        val errorArgs = Seq(formatDateToString(maxDate), "date.error.year") ++ args
        Left(List(FormError(key, futureDateKey, errorArgs)))
      } else if (date.isBefore(minDate)) {
        val errorArgs = Seq("date.error.year") ++ args
        Left(List(FormError(key, pastDateKey, errorArgs)))
      } else {
        Right(date)
      }
    }

  private def twoFieldsMissing(key: String, missingFields: => List[String]) =
    if (!missingFields.exists(_.toLowerCase.contains("day"))) {
      Left(List(FormError(key, monthAndYearRequiredKey, missingFields ++ args)))
    } else if (!missingFields.exists(_.toLowerCase.contains("month"))) {
      Left(List(FormError(key, dayAndYearRequiredKey, missingFields ++ args)))
    } else {
      Left(List(FormError(key, dayAndMonthRequiredKey, missingFields ++ args)))
    }

  private def singleFieldMissing(key: String, missingFields: List[String]): Either[Seq[FormError], LocalDate] =
    if (missingFields.exists(_.toLowerCase.contains("day"))) {
      Left(List(FormError(key, dayRequiredKey, Seq(messages("date.error.day")) ++ args)))
    } else if (missingFields.exists(_.toLowerCase.contains("month"))) {
      Left(List(FormError(key, monthRequiredKey, Seq(messages("date.error.month")) ++ args)))
    } else {
      Left(List(FormError(key, yearRequiredKey, Seq(messages("date.error.year")) ++ args)))
    }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )

  private class MonthFormatter(invalidKey: String, args: Seq[String] = Seq.empty)
      extends Formatter[Int]
      with Formatters {
    private val baseFormatter                                                              = stringFormatter(invalidKey, args)
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] = {
      val months = Month.values.toList
      baseFormatter
        .bind(key, data)
        .flatMap { str =>
          months
            .find(m =>
              m.getValue.toString == str.replaceAll("^0+", "")
                || m.toString == str.toUpperCase
                || m.toString.take(3) == str.toUpperCase
            )
            .map(x => Right(x.getValue))
            .getOrElse(Left(List(FormError(key, invalidKey, args))))
        }
    }
    override def unbind(key: String, value: Int): Map[String, String]                      =
      Map(key -> value.toString)
  }
}
