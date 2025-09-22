package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class IndividualRegistrationTypeSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "IndividualRegistrationType" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(IndividualRegistrationType.values.toSeq)

      forAll(gen) {
        individualRegistrationType =>

          JsString(individualRegistrationType.toString).validate[IndividualRegistrationType].asOpt.value mustEqual individualRegistrationType
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!IndividualRegistrationType.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[IndividualRegistrationType] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(IndividualRegistrationType.values.toSeq)

      forAll(gen) {
        individualRegistrationType =>

          Json.toJson(individualRegistrationType) mustEqual JsString(individualRegistrationType.toString)
      }
    }
  }
}
