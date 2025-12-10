package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class OrganisationBusinessAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new OrganisationBusinessAddressFormProvider()()

  ".AddressLine1" - {

    val fieldName   = "AddressLine1"
    val requiredKey = "organisationBusinessAddress.error.AddressLine1.required"
    val lengthKey   = "organisationBusinessAddress.error.AddressLine1.length"
    val maxLength   = 35

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

  ".AddressLine2" - {

    val fieldName   = "AddressLine2"
    val requiredKey = "organisationBusinessAddress.error.AddressLine2.required"
    val lengthKey   = "organisationBusinessAddress.error.AddressLine2.length"
    val maxLength   = 35

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
