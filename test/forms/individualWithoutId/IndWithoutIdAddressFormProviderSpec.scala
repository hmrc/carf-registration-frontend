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

import config.Constants.{addressMaxLength, addressRegex}
import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError
import utils.CountryListFactory

class IndWithoutIdAddressFormProviderSpec extends StringFieldBehaviours {

  private val formProvider = new IndWithoutIdAddressFormProvider()
  private val form         = formProvider(CountryListFactory.ukCountries)

  val validAddressStringGen: Gen[String] = {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789 &.,'-"
    for {
      length <- Gen.choose(1, addressMaxLength)
      chars  <- Gen.listOfN(length, Gen.oneOf(allowedChars))
    } yield chars.mkString
  }.suchThat(_ != " ")

  val baseFormData: Map[String, String] = Map(
    "addressLine1" -> "1 Test Street",
    "townOrCity"   -> "Testville"
  )

  ".addressLine1" - {
    val fieldName   = "addressLine1"
    val requiredKey = "address.addressLine1.error.required"
    val lengthKey   = "address.addressLine1.error.length"
    val invalidKey  = "address.addressLine1.error.invalid"
    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)

    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(addressMaxLength)))
    }
    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))
    "must not bind strings with invalid characters" in {
      val invalidString = "123 Street!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors.head.key     mustBe fieldName
      result.errors.head.message mustBe invalidKey
    }
  }

  ".addressLine2" - {
    val fieldName  = "addressLine2"
    val lengthKey  = "address.addressLine2.error.length"
    val invalidKey = "address.addressLine2.error.invalid"
    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)
    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(addressMaxLength)))
    }
    "must bind an empty string as valid" in {
      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors mustBe empty
    }
    "must not bind strings with invalid characters" in {
      val invalidString = "Apt 4!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq(addressRegex)))
    }
  }

  ".addressLine3" - {
    val fieldName  = "addressLine3"
    val lengthKey  = "address.addressLine3.error.length"
    val invalidKey = "address.addressLine3.error.invalid"
    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)
    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(addressMaxLength)))
    }
    "must bind an empty string as valid" in {
      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors mustBe empty
    }
    "must not bind strings with invalid characters" in {
      val invalidString = "Apt 4!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq(addressRegex)))
    }
  }

  ".townOrCity" - {
    val fieldName   = "townOrCity"
    val requiredKey = "address.townOrCity.error.required"
    val lengthKey   = "address.townOrCity.error.length"
    val invalidKey  = "address.townOrCity.error.invalid"

    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)

    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Seq(addressMaxLength)))
    }

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "must not bind strings with invalid characters" in {
      val invalidString = "Luton!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq(addressRegex)))
    }
  }

  ".postcode" - {
    "must return a required error if postcode is empty" in {
      val formData = baseFormData ++ Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "JE",
        "postcode"     -> ""
      )
      val result   = form.bind(formData)

      result.errors must contain(
        FormError("postcode", "address.postcode.error.required")
      )
    }

    "must return a length error if postcode is too long" in {
      val postcode = "A" * 20
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "GB",
        "postcode"     -> postcode
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.length"))
    }

    "must return an invalid character error if postcode contains invalid chars" in {
      val postcode = "!!??"
      val formData = baseFormData ++ Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "JE",
        "postcode"     -> postcode
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalid"))
    }

    "must return an 'invalid format' error for N123456" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "JE",
        "postcode"     -> "N123456"
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalidFormat"))
    }

    "must return an 'invalid format' if isle of man as country is entered but Jersey postcode is present" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "IM",
        "postcode"     -> "JE4 1AA"
      )

      val result = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalidFormat"))
    }

    "must return an 'invalid format' if UK(GB) as country is entered but Jersey postcode is present" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "GB",
        "postcode"     -> "JE4 1AA"
      )

      val result = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalidFormat"))
    }

    "must return an 'invalid format' if UK(GB) as country is entered but Jersey postcode is not real" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "GB",
        "postcode"     -> "JE0 1AA"
      )

      val result = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalidFormat"))
    }

    "must return an 'invalid format' if IM as country is entered but Jersey postcode is not real" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "IM",
        "postcode"     -> "JE0 1AA"
      )

      val result = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalidFormat"))
    }

    "must return an 'invalid format' if IM as country is entered but UK postcode is present" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "IM",
        "postcode"     -> "EC4R 9AT"
      )

      val result = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.invalidFormat"))
    }

    "must return a 'real postcode' error for example postcode AA1 1AA" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "JE",
        "postcode"     -> "AA1 1AA"
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.notReal"))
    }

    "must return a 'real postcode' error for postcode AA11AA (no spaces)" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "UK",
        "postcode"     -> "AA11AA"
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.notReal"))
    }

    "must return a 'real postcode' error for example postcode JE5 1AA" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "JE",
        "postcode"     -> "JE5 1AA"
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.notReal"))
    }

    "must return a 'real postcode' error for example postcode JE51AA without space" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "JE",
        "postcode"     -> "JE51AA"
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "address.postcode.error.notReal"))
    }

    "must be valid if postcode is provided in a valid format" in {
      val formData =
        Map("addressLine1" -> "addressLine1", "townOrCity" -> "town", "country" -> "GB", "postcode" -> "NW4 1QS")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }

    "must be valid if postcode is provided in a valid format in lowercase" in {
      val formData =
        Map("addressLine1" -> "addressLine1", "townOrCity" -> "town", "country" -> "GB", "postcode" -> "NW4 1qs")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }

    "must be valid if a Birmingham postcode is provided in a valid format" in {
      val formData =
        Map("addressLine1" -> "addressLine1", "townOrCity" -> "town", "country" -> "GB", "postcode" -> "B23 2AZ")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }
  }

  ".country" - {
    val fieldName   = "country"
    val requiredKey = "address.country.error.required"

    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))

    "must not bind a non-uk country code" in {
      val data   = baseFormData ++ Map(fieldName -> "FR")
      val result = form.bind(data).apply(fieldName)
      result.errors must contain(FormError(fieldName, requiredKey))
    }

    "must bind a valid country code" in {
      val data   = baseFormData ++ Map(fieldName -> "GB")
      val result = form.bind(data).apply(fieldName)
      result.errors mustBe empty
    }
  }

  "combinations" - {
    "must only show country required error when valid postcode is provided and country is empty" in {
      val formData = Map(
        "addressLine1" -> "addressLine1",
        "townOrCity"   -> "town",
        "country"      -> "",
        "postcode"     -> "FX4 7AL"
      )
      val result   = form.bind(formData)
      result.errors must contain(FormError("country", "address.country.error.required"))

    }
  }
}
