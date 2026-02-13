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
import models.IndWithoutIdAddressNonUk
import models.countries.Country
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
      raw    <- Gen.listOfN(length, Gen.oneOf(allowedChars)).map(_.mkString)
      trimmed = raw.trim
      if trimmed.nonEmpty
    } yield trimmed
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
      result.errors must contain(FormError(fieldName, lengthKey))
    }

    "must bind an empty string as valid" in {
      val result = form.bind(baseFormData + (fieldName -> ""))
      result.hasErrors mustBe false
    }

    "must not bind strings with invalid characters" in {
      val invalidString = "Apt 4!"
      val result        = form.bind(baseFormData + (fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey))
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

    "must bind when value has leading/trailing spaces" in {
      val value  = "  Paris  "
      val result = form.bind(baseFormData + (fieldName -> value)).apply(fieldName)
      result.errors mustBe empty
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
      result.errors must contain(FormError(fieldName, lengthKey))
    }

    "must bind an empty string as valid" in {
      val result = form.bind(baseFormData + (fieldName -> ""))
      result.hasErrors mustBe false
    }

    "must not bind strings with invalid characters" in {
      val invalidString = "Ile-de-France!"
      val result        = form.bind(baseFormData + (fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey))
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
      result.errors must contain(FormError(fieldName, lengthKey))
    }

    "must bind an empty string as valid" in {
      val result = form.bind(baseFormData + (fieldName -> ""))
      result.hasErrors mustBe false
    }

    "must not bind strings with invalid characters" in {
      val invalidString = "12345!"
      val result        = form.bind(baseFormData + (fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey))
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
  "form binding with all valid fields populates the model and has no errors" in {
    val data   = Map(
      "addressLine1" -> "123 Main Street",
      "addressLine2" -> "Apt 4B",
      "townOrCity"   -> "Paris",
      "region"       -> "Ile-de-France",
      "postcode"     -> "75001",
      "country"      -> "FR"
    )
    val result = form.bind(data)
    result.hasErrors mustBe false

    val expected = IndWithoutIdAddressNonUk(
      addressLine1 = "123 Main Street",
      addressLine2 = Some("Apt 4B"),
      townOrCity = "Paris",
      region = Some("Ile-de-France"),
      postcode = Some("75001"),
      country = france
    )
    result.get mustBe expected
  }

  "form binding should populate model correctly after binding" in {
    val data  = Map(
      "addressLine1" -> "456 Rue Example",
      "addressLine2" -> "Suite 300",
      "townOrCity"   -> "Lyon",
      "region"       -> "Auvergne-Rhone-Alpes",
      "postcode"     -> "69000",
      "country"      -> "FR"
    )
    val bound = form.bind(data).get
    bound.addressLine1 mustBe "456 Rue Example"
    bound.addressLine2 mustBe Some("Suite 300")
    bound.townOrCity   mustBe "Lyon"
    bound.region       mustBe Some("Auvergne-Rhone-Alpes")
    bound.postcode     mustBe Some("69000")
    bound.country      mustBe france
  }

  "form binding with single country in country list" in {
    val singleCountryForm = new IndWithoutIdAddressNonUkFormProvider()(Seq(germany))
    val data              = baseFormData + ("country" -> "DE")
    val result            = singleCountryForm.bind(data)
    result.hasErrors   mustBe false
    result.get.country mustBe germany
  }

  "form binding with empty country list always errors on country" in {
    val emptyCountryForm = new IndWithoutIdAddressNonUkFormProvider()(Seq.empty)
    val data             = baseFormData
    val result           = emptyCountryForm.bind(data)
    result.errors.map(_.key) must contain("country")
  }

  "form binding ignores extra fields" in {
    val data   = baseFormData + ("extraField" -> "shouldBeIgnored")
    val result = form.bind(data)
    result.hasErrors        mustBe false
    result.get.addressLine1 mustBe "123 Main Street"
  }

  "form binding with missing optional fields does not error" in {
    val data   = Map(
      "addressLine1" -> "123 Main Street",
      "townOrCity"   -> "Paris",
      "country"      -> "FR"
    )
    val result = form.bind(data)
    result.hasErrors        mustBe false
    result.get.addressLine2 mustBe None
    result.get.region       mustBe None
    result.get.postcode     mustBe None
  }

  "form binding trims leading/trailing spaces for required fields" in {
    val data   = Map(
      "addressLine1" -> "   123 Main Street   ",
      "townOrCity"   -> "  Paris ",
      "country"      -> "FR"
    )
    val result = form.bind(data)
    result.hasErrors        mustBe false
    result.get.addressLine1 mustBe "123 Main Street"
    result.get.townOrCity   mustBe "Paris"
  }

  "form binding fails on a variety of invalid characters" in {
    val invalids = Seq("abc@", "--/()", "John#Doe", "Street*Name")
    invalids.foreach { bad =>
      val result = form.bind(baseFormData + ("addressLine1" -> bad)).apply("addressLine1")
      result.errors must not be empty
    }
  }
}
