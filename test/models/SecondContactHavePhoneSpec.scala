package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class SecondContactHavePhoneSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "SecondContactHavePhone" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SecondContactHavePhone.values.toSeq)

      forAll(gen) {
        secondContactHavePhone =>

          JsString(secondContactHavePhone.toString).validate[SecondContactHavePhone].asOpt.value mustEqual secondContactHavePhone
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!SecondContactHavePhone.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[SecondContactHavePhone] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SecondContactHavePhone.values.toSeq)

      forAll(gen) {
        secondContactHavePhone =>

          Json.toJson(secondContactHavePhone) mustEqual JsString(secondContactHavePhone.toString)
      }
    }
  }
}
