package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class NiNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "niNumber.error.required"
  val invalidFormatKey = "niNumber.error.invalidFormat"
  val invalidKey = "niNumber.error.invalid"

  val form = new NiNumberFormProvider()()
  
  ".ni-number" - {

    val fieldName = "ni-number"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
    
    "must bind NINOs that pass both regex checks" in {
      val validNinos = Seq(
        "AB123456C",
        "BA123456A",
        "CE123456A",
        "GH123456B"
      )

      validNinos.foreach { validNino =>
        val result = form.bind(Map(fieldName -> validNino))
        result.errors mustBe empty
        result.get mustEqual validNino
      }
    }
    
    "must not bind NINOs that fail ninoFormatRegex regex - Invalid Format Error" in {
      val invalidFormats = Seq(
        "A1234567B",     // 8
        "AB12345678C",   // 10
        "1B123456C",     // starts with int
        "A1123456C",     // second digit is int
        "AB1234567",     // 9 but doesn't end in letter
        "AB-123456-C",   // hyphens
        "ABCDEFGHI",     // All letters
        "123456789"      // All numbers
      )

      invalidFormats.foreach { invalidNino =>
        val result = form.bind(Map(fieldName -> invalidNino))
        result.errors must contain(FormError(fieldName, invalidFormatKey))
      }
    }

    "must not bind NINOs that fail ninoRegex - have invalid prefixes (like example QQ123456C) - Not Real Error" in {
      val notReal = Seq(
        "QQ123456C",
        "BG123456C",
        "GB123456C",
        "NK123456C"
      )

      notReal.foreach { invalidNino =>
        val result = form.bind(Map(fieldName -> invalidNino))
        result.errors must contain(FormError(fieldName, invalidKey))
      }
    }

    "must normalize user input (spaces, convert to uppercase)" in {
      val normalizationCases = Seq(
        ("AB 12 34 56 C", "AB123456C"),
        ("ab123456c", "AB123456C"),
        ("Ab123456C", "AB123456C"),
        ("AB123456 C", "AB123456C"),
        (" AB123456C", "AB123456C")
      )

      normalizationCases.foreach { case (input, expected) =>
        val result = form.bind(Map(fieldName -> input))
        result.errors mustBe empty
        result.get mustEqual expected
      }
    }
  }
}