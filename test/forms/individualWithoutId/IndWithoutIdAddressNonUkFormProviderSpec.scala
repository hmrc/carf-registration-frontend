/*
 * Copyright 2026 HM Revenue & Customs
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

package forms.individualWithoutId

import config.Constants.*
import forms.behaviours.StringFieldBehaviours
import forms.individualWithoutId.IndWithoutIdAddressNonUkFormProvider
import models.{Country, IndWithoutIdAddressNonUk}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class IndWithoutIdAddressNonUkFormProviderSpec extends StringFieldBehaviours {

  val france: Country           = Country("FR", "France")
  val germany: Country          = Country("DE", "Germany")
  val countryList: Seq[Country] = Seq(france, germany)

  val form: Form[IndWithoutIdAddressNonUk] = new IndWithoutIdAddressNonUkFormProvider()(countryList)

  val validAddressStringGen: Gen[String] = {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 &.,'-"
    for {
      length <- Gen.choose(1, addressMaxLength)
      chars  <- Gen.listOfN(length, Gen.oneOf(allowedChars))
    } yield chars.mkString
  }

  val validPostcodeStringGen: Gen[String] = {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 "
    for {
      length <- Gen.choose(1, 10)
      chars  <- Gen.listOfN(length, Gen.oneOf(allowedChars))
    } yield chars.mkString
  }

  val baseFormData: Map[String, String] = Map(
    "addressLine1" -> "123 Main Street",
    "townOrCity"   -> "Paris",
    "country"      -> "FR"
  )

  ".addressLine1" - {
    val fieldName   = "addressLine1"
    val requiredKey = "indWithoutIdAddressNonUk.addressLine1.error.required"
    val lengthKey   = "indWithoutIdAddressNonUk.addressLine1.error.length"
    val invalidKey  = "indWithoutIdAddressNonUk.addressLine1.error.invalid"

    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)

    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(baseFormData + (fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, List()))
    }

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "must not bind strings with invalid characters" in {
      val invalidString = "123 Street!"
      val result        = form.bind(baseFormData + (fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, List()))
    }
  }

  ".addressLine2" - {
    val fieldName  = "addressLine2"
    val lengthKey  = "indWithoutIdAddressNonUk.addressLine2.error.length"
    val invalidKey = "indWithoutIdAddressNonUk.addressLine2.error.invalid"

    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)

    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(baseFormData + (fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(addressMaxLength)))
    }

    "must bind an empty string as valid" in {
      val result = form.bind(baseFormData + (fieldName -> ""))
      result.hasErrors mustBe false
    }

    "must not bind strings with invalid characters" in {
      val invalidString = "Apt 4!"
      val result        = form.bind(baseFormData + (fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq(addressRegex)))
    }
  }

  ".townOrCity" - {
    val fieldName   = "townOrCity"
    val requiredKey = "indWithoutIdAddressNonUk.townOrCity.error.required"
    val lengthKey   = "indWithoutIdAddressNonUk.townOrCity.error.length"
    val invalidKey  = "indWithoutIdAddressNonUk.townOrCity.error.invalid"

    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)

    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(baseFormData + (fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, List()))
    }

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "must not bind strings with invalid characters" in {
      val invalidString = "Paris!"
      val result        = form.bind(baseFormData + (fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, List()))
    }
  }

  ".region" - {
    val fieldName  = "region"
    val lengthKey  = "indWithoutIdAddressNonUk.region.error.length"
    val invalidKey = "indWithoutIdAddressNonUk.region.error.invalid"

    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)

    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(baseFormData + (fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(addressMaxLength)))
    }

    "must bind an empty string as valid" in {
      val result = form.bind(baseFormData + (fieldName -> ""))
      result.hasErrors mustBe false
    }

    "must not bind strings with invalid characters" in {
      val invalidString = "Ile-de-France!"
      val result        = form.bind(baseFormData + (fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq(addressRegex)))
    }
  }

  ".postcode" - {
    val fieldName  = "postcode"
    val lengthKey  = "indWithoutIdAddressNonUk.postcode.error.length"
    val invalidKey = "indWithoutIdAddressNonUk.postcode.error.invalid"

    behave like fieldThatBindsValidData(form, fieldName, validPostcodeStringGen)

    "must not bind strings longer than 10 characters" in {
      val longString = "a" * 11
      val result     = form.bind(baseFormData + (fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(10)))
    }

    "must bind an empty string as valid" in {
      val result = form.bind(baseFormData + (fieldName -> ""))
      result.hasErrors mustBe false
    }

    "must not bind strings with invalid characters" in {
      val invalidString = "12345!"
      val result        = form.bind(baseFormData + (fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq("^[A-Za-z0-9 ]*$")))
    }
  }

  ".country" - {
    val fieldName   = "country"
    val requiredKey = "indWithoutIdAddressNonUk.country.error.required"

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "must not bind an invalid country code" in {
      val result = form.bind(baseFormData + (fieldName -> "ZZ")).apply(fieldName)
      result.errors must contain(FormError(fieldName, requiredKey))
    }

    "must bind a valid country code" in {
      val result = form.bind(baseFormData + (fieldName -> "FR")).apply(fieldName)
      result.errors mustBe empty
    }
  }
}
