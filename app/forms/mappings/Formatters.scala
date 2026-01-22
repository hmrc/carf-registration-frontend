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

import com.google.i18n.phonenumbers.{NumberParseException, PhoneNumberUtil, Phonenumber}
import config.Constants
import config.Constants.{maxPhoneLength, ninoFormatRegex, ninoRegex}
import models.Enumerable
import play.api.Logging
import play.api.data.FormError
import play.api.data.format.Formatter

import scala.util.control.Exception.nonFatalCatch
import scala.util.{Failure, Success, Try}

trait Formatters extends Transforms with Logging {

  private type EitherFormErrorOrValue = Either[Seq[FormError], String]

  private[mappings] def stringFormatter(errorKey: String, args: Seq[String] = Seq.empty): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): EitherFormErrorOrValue =
        data.get(key) match {
          case None                      => Left(Seq(FormError(key, errorKey, args)))
          case Some(s) if s.trim.isEmpty => Left(Seq(FormError(key, errorKey, args)))
          case Some(s)                   => Right(s)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def booleanFormatter(
      requiredKey: String,
      invalidKey: String,
      args: Seq[String] = Seq.empty
  ): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true"  => Right(true)
            case "false" => Right(false)
            case _       => Left(Seq(FormError(key, invalidKey, args)))
          }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

  private[mappings] def intFormatter(
      requiredKey: String,
      wholeNumberKey: String,
      nonNumericKey: String,
      args: Seq[String] = Seq.empty
  ): Formatter[Int] =
    new Formatter[Int] {

      val decimalRegexp = """^-?(\d*\.\d*)$"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .flatMap {
            case s if s.matches(decimalRegexp) =>
              Left(Seq(FormError(key, wholeNumberKey, args)))
            case s                             =>
              nonFatalCatch
                .either(s.toInt)
                .left
                .map(_ => Seq(FormError(key, nonNumericKey, args)))
          }

      override def unbind(key: String, value: Int): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty)(
      implicit ev: Enumerable[A]
  ): Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap { str =>
          ev.withName(str)
            .map(Right.apply)
            .getOrElse(Left(Seq(FormError(key, invalidKey, args))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def currencyFormatter(
      requiredKey: String,
      invalidNumericKey: String,
      nonNumericKey: String,
      args: Seq[String] = Seq.empty
  ): Formatter[BigDecimal] =
    new Formatter[BigDecimal] {
      val isNumeric    = """(^£?\d*$)|(^£?\d*\.\d*$)"""
      val validDecimal = """(^£?\d*$)|(^£?\d*\.\d{1,2}$)"""

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], BigDecimal] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", "").replace(" ", ""))
          .flatMap {
            case s if !s.matches(isNumeric)    =>
              Left(Seq(FormError(key, nonNumericKey, args)))
            case s if !s.matches(validDecimal) =>
              Left(Seq(FormError(key, invalidNumericKey, args)))
            case s                             =>
              nonFatalCatch
                .either(BigDecimal(s.replace("£", "")))
                .left
                .map(_ => Seq(FormError(key, nonNumericKey, args)))
          }

      override def unbind(key: String, value: BigDecimal): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  protected def validatedUtrFormatter(
      requiredKey: String,
      invalidKey: String,
      invalidFormatKey: String,
      regex: String,
      msgArg: String = ""
  ): Formatter[String] =
    new Formatter[String] {

      val acceptedLengths: Seq[Int] = Constants.acceptedUtrLengths

      def formatError(key: String, errorKey: String, msgArg: String): FormError =
        if (msgArg.isEmpty) FormError(key, errorKey) else FormError(key, errorKey, Seq(msgArg))

      override def bind(key: String, data: Map[String, String]): EitherFormErrorOrValue = {
        val trimmedUtr = data.get(key).map(string => string.replaceAll("\\s", "").replaceAll("^[kK]+|[kK]+$", ""))
        trimmedUtr match {
          case None | Some("")                                => Left(Seq(formatError(key, requiredKey, msgArg)))
          case Some(s) if !s.matches(regex)                   => Left(Seq(formatError(key, invalidKey, msgArg)))
          case Some(s) if !acceptedLengths.contains(s.length) => Left(Seq(formatError(key, invalidFormatKey, msgArg)))
          case Some(s)                                        => Right(s)
        }
      }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)

    }

  private def removeNonBreakingSpaces(str: String) =
    str.replaceAll("\u00A0", " ")

  private[mappings] def stringTrimFormatter(errorKey: String, msgArg: String = ""): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): EitherFormErrorOrValue =
        data.get(key) match {
          case None    =>
            handleEmptyInput(key)
          case Some(s) =>
            s.trim match {
              case "" =>
                handleEmptyInput(key)
              case s1 => Right(removeNonBreakingSpaces(s1))
            }
        }

      private def handleEmptyInput(key: String) =
        if (msgArg.isEmpty) {
          Left(Seq(FormError(key, errorKey)))
        } else {
          Left(Seq(FormError(key, errorKey, Seq(msgArg))))
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)

    }

  protected def nationalInsuranceNumberFormatter(
      requiredKey: String,
      invalidFormatKey: String,
      invalidKey: String,
      args: Seq[Any] = Seq.empty
  ): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): EitherFormErrorOrValue =
        data.get(key) match {
          case None                              =>
            Left(Seq(FormError(key, requiredKey, args)))
          case Some(value) if value.trim.isEmpty =>
            Left(Seq(FormError(key, requiredKey, args)))
          case Some(value)                       =>
            val normalized = value.replaceAll("\\s", "").toUpperCase

            if (normalized.length > 9) {
              Left(Seq(FormError(key, invalidFormatKey, args)))
            } else if (!normalized.matches(ninoFormatRegex)) {
              Left(Seq(FormError(key, invalidFormatKey, args)))
            } else if (!normalized.matches(ninoRegex)) {
              Left(Seq(FormError(key, invalidKey, args)))
            } else {
              Right(normalized)
            }
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)

    }

  protected def phoneNumberFormatter(
      requiredKey: String,
      invalidKey: String,
      lengthKey: String,
      notRealPhoneNumberKey: String,
      args: Seq[Any] = Seq.empty
  ): Formatter[String] =
    new Formatter[String] {

      private val phoneUtil = PhoneNumberUtil.getInstance()

      override def bind(key: String, data: Map[String, String]): EitherFormErrorOrValue =
        lazy val formErrorInvalidKey = Left(Seq(FormError(key, invalidKey, args)))

        data.get(key).map(_.trim) match {
          case None                         => Left(Seq(FormError(key, requiredKey, args)))
          case Some(value) if value.isEmpty => Left(Seq(FormError(key, requiredKey, args)))
          case Some(value)                  =>
            if (value.length > maxPhoneLength) {
              Left(Seq(FormError(key, lengthKey, args)))
            } else {
              Try {
                val number = phoneUtil.parse(value, "GB")
                (phoneUtil.isPossibleNumber(number), phoneUtil.isValidNumber(number)) match {
                  case (true, true)  => validateNot0808Number(phoneUtil, key, value, notRealPhoneNumberKey, number, args)
                  case (true, false) => Left(Seq(FormError(key, notRealPhoneNumberKey, args)))
                  case (false, _)    => formErrorInvalidKey
                }
              } match {
                case Success(value)                   => value
                case Failure(_: NumberParseException) => formErrorInvalidKey
                case Failure(exception)               =>
                  logger.error(s"Unexpected phone number form error occurred with message: ${exception.getMessage}")
                  formErrorInvalidKey
              }
            }
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  /** To deal with the possibility that a user *MIGHT* respond to the Invalid error message ["Enter a phone number, like
    * 01632 960 001, 07700 900 982 or +44 808 157 0192"], by inputting "0808 157 0192" or "+44 808 157 0192" or
    * "+448081570192" etc, we explicitly give 'not real' error for these cases. This is because the google
    * PhoneNumberUtil validator does not consider "+44 808 157 0192" etc to be not Real, but correctly considers 01632
    * 960 001 & 07700 900 982 as not Real numbers.
    */

  protected def validateNot0808Number(
      phoneUtil: PhoneNumberUtil,
      key: String,
      value: String,
      notRealErrorKey: String,
      number: Phonenumber.PhoneNumber,
      args: Seq[Any] = Seq.empty
  ): Either[Seq[FormError], String] = {
    val formattedNumber = phoneUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164)

    if (formattedNumber == Constants.notReal0808PhoneNumber) {
      Left(Seq(FormError(key, notRealErrorKey, args)))
    } else {
      Right(value)
    }
  }

  protected def validatedTextFormatter(
      requiredKey: String,
      invalidKey: String,
      lengthKey: String,
      regex: String,
      maxLength: Int,
      minLength: Int = 1,
      msgArg: String = ""
  ): Formatter[String] =
    new Formatter[String] {
      private val dataFormatter: Formatter[String] = stringTrimFormatter(requiredKey, msgArg)

      override def bind(key: String, data: Map[String, String]): EitherFormErrorOrValue =
        dataFormatter
          .bind(key, data)
          .flatMap {
            case str if str.length > maxLength => Left(Seq(FormError(key, lengthKey)))
            case str if str.length < minLength => Left(Seq(FormError(key, lengthKey)))
            case str if !str.matches(regex)    => Left(Seq(FormError(key, invalidKey)))
            case str                           => Right(str)
          }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def mandatoryPostcodeFormatter(
      requiredKey: String,
      lengthKey: String,
      invalidKey: String,
      regex: String,
      invalidCharKey: String,
      validCharRegex: String
  ): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
        val postCode          = data.get(key).map(_.trim.replaceAll("\\s+", ""))
        val maxLengthPostcode = 10

        postCode match {
          case Some(postCode) if postCode.isEmpty                    => Left(Seq(FormError(key, requiredKey)))
          case Some(postCode) if !postCode.matches(validCharRegex)   => Left(Seq(FormError(key, invalidCharKey)))
          case Some(postCode) if !postCode.matches(regex)            => Left(Seq(FormError(key, invalidKey)))
          case Some(postCode) if postCode.length > maxLengthPostcode => Left(Seq(FormError(key, lengthKey)))
          case Some(postCode)                                        => Right(validPostCodeFormat(postCode))
          case _                                                     => Left(Seq(FormError(key, requiredKey)))
        }
      }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)

    }
}
