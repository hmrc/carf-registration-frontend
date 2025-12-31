package forms.individualWithoutId

import config.CurrencyFormatter.currencyFormat
import forms.behaviours.CurrencyFieldBehaviours
import forms.individualWithoutId.IndWithoutIdDateOfBirthFormProvider
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class IndWithoutIdDateOfBirthFormProviderSpec extends CurrencyFieldBehaviours {

  val form = new IndWithoutIdDateOfBirthFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = Int.MaxValue

    val validDataGenerator =
      Gen.choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError     = FormError(fieldName, "indWithoutIdDateOfBirth.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "indWithoutIdDateOfBirth.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(fieldName, "indWithoutIdDateOfBirth.error.aboveMaximum", Seq(currencyFormat(maximum)))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "indWithoutIdDateOfBirth.error.required")
    )
  }
}
