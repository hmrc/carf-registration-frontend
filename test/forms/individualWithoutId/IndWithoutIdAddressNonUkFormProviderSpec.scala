package forms.individualWithoutId

import forms.behaviours.StringFieldBehaviours
import forms.individualWithoutId.IndWithoutIdAddressNonUkFormProvider
import play.api.data.FormError

class IndWithoutIdAddressNonUkFormProviderSpec extends StringFieldBehaviours {

  val form = new IndWithoutIdAddressNonUkFormProvider()()

  ".Address1" - {

    val fieldName = "Address1"
    val requiredKey = "indWithoutIdAddressNonUk.error.Address1.required"
    val lengthKey = "indWithoutIdAddressNonUk.error.Address1.length"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".Address2" - {

    val fieldName = "Address2"
    val requiredKey = "indWithoutIdAddressNonUk.error.Address2.required"
    val lengthKey = "indWithoutIdAddressNonUk.error.Address2.length"
    val maxLength = 35

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
