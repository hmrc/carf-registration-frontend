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

package forms.orgWithoutId

import config.Constants.*
import forms.behaviours.StringFieldBehaviours
import models.countries.*
import models.OrganisationBusinessAddress
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class OrganisationBusinessAddressFormProviderSpec extends StringFieldBehaviours {

  val uk: Country               = Country("GB", "United Kingdom")
  val france: Country           = Country("FR", "France")
  val jersey: Country           = Country("JE", "Jersey")
  val guernsey: Country         = Country("GG", "Guernsey")
  val isleOfMan: Country        = Country("IM", "Isle of Man")
  val countryList: Seq[Country] = Seq(uk, france, jersey, guernsey, isleOfMan)

  val form: Form[OrganisationBusinessAddress] = new OrganisationBusinessAddressFormProvider()(countryList)

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
    val requiredKey = "organisationBusinessAddress.addressLine1.error.required"
    val lengthKey   = "organisationBusinessAddress.addressLine1.error.length"
    val invalidKey  = "organisationBusinessAddress.addressLine1.error.invalid"
    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)
    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Nil))
    }
    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))
    "must not bind strings with invalid characters" in {
      val invalidString = "123 Street!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Nil))
    }
  }

  ".addressLine2" - {
    val fieldName  = "addressLine2"
    val lengthKey  = "organisationBusinessAddress.addressLine2.error.length"
    val invalidKey = "organisationBusinessAddress.addressLine2.error.invalid"
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
    val requiredKey = "organisationBusinessAddress.townOrCity.error.required"
    val lengthKey   = "organisationBusinessAddress.townOrCity.error.length"
    val invalidKey  = "organisationBusinessAddress.townOrCity.error.invalid"
    behave like fieldThatBindsValidData(form, fieldName, validAddressStringGen)
    "must not bind strings longer than the max length" in {
      val longString = "a" * (addressMaxLength + 1)
      val result     = form.bind(Map(fieldName -> longString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, lengthKey, Nil))
    }
    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))
    "must not bind strings with invalid characters" in {
      val invalidString = "Luton!"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Nil))
    }
  }

  ".region" - {
    val fieldName  = "region"
    val lengthKey  = "organisationBusinessAddress.region.error.length"
    val invalidKey = "organisationBusinessAddress.region.error.invalid"
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
      val invalidString = "California*"
      val result        = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey, Seq(addressRegex)))
    }
  }

  ".postcode" - {
    "must return a required error if country is a Crown Dependency and postcode is empty" in {
      val formData = baseFormData ++ Map("country" -> "JE", "postcode" -> "")
      val result   = form.bind(formData)
      result.errors must contain(
        FormError("postcode", "organisationBusinessAddress.postcode.error.emptyAndCountryIsCrownDependency")
      )
    }

    "must return a length error if postcode is too long" in {
      val postcode = "A" * 20
      val formData = baseFormData ++ Map("country" -> "FR", "postcode" -> postcode)
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "organisationBusinessAddress.postcode.error.length"))
    }

    "must return an invalid character error if postcode contains invalid chars" in {
      val postcode = "!!??"
      val formData = baseFormData ++ Map("country" -> "JE", "postcode" -> postcode)
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "organisationBusinessAddress.postcode.error.invalid"))
    }

    "must return an 'invalid format' error if country is a Crown Dependency and postcode has wrong starting letters" in {
      val formData = baseFormData ++ Map("country" -> "JE", "postcode" -> "GY1 1AA")
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "organisationBusinessAddress.postcode.error.invalidFormat"))
    }

    "must return a 'real postcode' error for example postcode AA1 1AA when country is Crown Dependency" in {
      val formData = baseFormData ++ Map("country" -> "JE", "postcode" -> "AA1 1AA")
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "organisationBusinessAddress.postcode.error.required"))
    }

    "must accept example postcode AA1 1AA when country is not a Crown Dependency" in {
      val formData = baseFormData ++ Map("country" -> "FR", "postcode" -> "AA1 1AA")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }

    "must return a 'real postcode' error for Jersey with invalid district JE5" in {
      val formData = baseFormData ++ Map("country" -> "JE", "postcode" -> "JE5 1AA")
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "organisationBusinessAddress.postcode.error.required"))
    }

    "must return a 'real postcode' error for Jersey with invalid district JE0" in {
      val formData = baseFormData ++ Map("country" -> "JE", "postcode" -> "JE0 1AA")
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "organisationBusinessAddress.postcode.error.required"))
    }

    "must return a 'real postcode' error for Isle of Man with invalid district IM0" in {
      val formData = baseFormData ++ Map("country" -> "IM", "postcode" -> "IM0 1AA")
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "organisationBusinessAddress.postcode.error.required"))
    }

    "must return a 'real postcode' error for Guernsey with invalid district GY0" in {
      val formData = baseFormData ++ Map("country" -> "GG", "postcode" -> "GY0 1AA")
      val result   = form.bind(formData)
      result.errors must contain(FormError("postcode", "organisationBusinessAddress.postcode.error.required"))
    }

    "must be valid for Jersey with valid districts JE1-JE4" in {
      Seq("JE1 1AA", "JE2 1AA", "JE3 1AA", "JE4 1AA").foreach { postcode =>
        val formData = baseFormData ++ Map("country" -> "JE", "postcode" -> postcode)
        val result   = form.bind(formData)
        result.hasErrors mustBe false
      }
    }

    "must be valid for Isle of Man with valid districts IM1-IM9 and IM99" in {
      Seq("IM1 1AA", "IM5 1AA", "IM9 1AA", "IM99 1AA").foreach { postcode =>
        val formData = baseFormData ++ Map("country" -> "IM", "postcode" -> postcode)
        val result   = form.bind(formData)
        result.hasErrors mustBe false
      }
    }

    "must be valid for Guernsey with valid districts GY1-GY10" in {
      Seq("GY1 1AA", "GY5 1AA", "GY10 1AA").foreach { postcode =>
        val formData = baseFormData ++ Map("country" -> "GG", "postcode" -> postcode)
        val result   = form.bind(formData)
        result.hasErrors mustBe false
      }
    }

    "must be valid if country is a Crown Dependency and postcode is provided in a valid format" in {
      val formData = baseFormData ++ Map("country" -> "JE", "postcode" -> "JE2 3AB")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }

    "must correctly format a Crown Dependency postcode by adding a space" in {
      val postcodeWithoutSpace = "JE23AB"
      val data                 = baseFormData ++ Map("country" -> "JE", "postcode" -> postcodeWithoutSpace)
      val result               = form.bind(data)
      result.hasErrors          mustBe false
      result.value.get.postcode mustBe Some("JE2 3AB")
    }

    "must correctly trim a non-Crown Dependency postcode" in {
      val postcodeWithSpaces = "  12345-6789  "
      val data               = baseFormData ++ Map("country" -> "FR", "postcode" -> postcodeWithSpaces)
      val result             = form.bind(data)
      result.hasErrors          mustBe false
      result.value.get.postcode mustBe Some("12345-6789")
    }

    "must preserve internal spaces and case for non-Crown Dependency postcodes" in {
      val postcodeWithInternalSpaces = "  abc   123  "
      val data                       = baseFormData ++ Map("country" -> "FR", "postcode" -> postcodeWithInternalSpaces)
      val result                     = form.bind(data)
      result.hasErrors          mustBe false
      result.value.get.postcode mustBe Some("abc   123")
    }

    "must be valid if country is not a Crown Dependency and postcode is empty" in {
      val formData = baseFormData ++ Map("country" -> "FR", "postcode" -> "")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }

    "must be valid if country is not a Crown Dependency and postcode is provided" in {
      val formData = baseFormData ++ Map("country" -> "GB", "postcode" -> "SW1A 0AA")
      val result   = form.bind(formData)
      result.hasErrors mustBe false
    }
  }

  ".country" - {
    val fieldName   = "country"
    val requiredKey = "organisationBusinessAddress.country.error.required"
    behave like mandatoryField(form, fieldName, requiredError = FormError(fieldName, requiredKey))
    "must not bind an invalid country code" in {
      val data   = baseFormData ++ Map(fieldName -> "ZZ")
      val result = form.bind(data).apply(fieldName)
      result.errors must contain(FormError(fieldName, requiredKey))
    }
    "must bind a valid country code" in {
      val data   = baseFormData ++ Map(fieldName -> "FR")
      val result = form.bind(data).apply(fieldName)
      result.errors mustBe empty
    }
  }
}
