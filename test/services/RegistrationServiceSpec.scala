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

package services

import base.SpecBase
import cats.data.EitherT
import connectors.RegistrationConnector
import models.JourneyType.{IndWithNino, IndWithUtr, IndWithoutId, OrgWithUtr, OrgWithoutId}
import models.error.ApiError.{InternalServerError, NotFoundError}
import models.error.{ApiError, CarfError, DataError}
import models.requests.*
import models.responses.{AddressRegistrationResponse, RegisterIndividualWithIdResponse, RegisterOrganisationWithIdResponse, RegisterWithoutIdResponse}
import models.{BusinessDetails, IndividualDetails, Name, RegistrationType, SafeId, UniqueTaxpayerReference, UserAnswers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalactic.Prettifier.default
import pages.WhereDoYouLivePage
import pages.individual.{IndividualEmailPage, IndividualHavePhonePage, IndividualPhoneNumberPage}
import pages.individualWithoutId.{IndWithoutIdAddressNonUkPage, IndWithoutIdDateOfBirthPage, IndWithoutIdUkAddressInUserAnswers, IndWithoutNinoNamePage}
import pages.orgWithoutId.{OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage}
import pages.organisation.{FirstContactEmailPage, FirstContactPhoneNumberPage, FirstContactPhonePage, RegistrationTypePage, UniqueTaxpayerReferenceInUserAnswers, WhatIsTheNameOfYourBusinessPage, WhatIsYourNamePage}

import java.time.LocalDate
import scala.concurrent.Future

class RegistrationServiceSpec extends SpecBase {
  val mockConnector: RegistrationConnector = mock[RegistrationConnector]
  val testService                          = new RegistrationService(mockConnector)
  val testOrgType: RegistrationType        = RegistrationType.LimitedCompany
  val ninoOkFullIndividualResponse         = "JX123456D"
  val ninoNotFound                         = "XX123456D"
  val ninoInternalServerError              = "YX123456D"
  val validBirthDate: LocalDate            = LocalDate.of(2000, 1, 1)

  val testAddress = AddressRegistrationResponse(
    addressLine1 = "123 Main Street",
    addressLine2 = Some("Birmingham"),
    addressLine3 = None,
    addressLine4 = None,
    postalCode = Some("B23 2AZ"),
    countryCode = "GB",
    countryName = None
  )

  val testRegisterIndividualWithIdSuccessResponse = RegisterIndividualWithIdResponse(
    safeId = "testSafeId",
    firstName = "Floriane",
    lastName = "Yammel",
    middleName = Some("Exie"),
    address = testAddress
  )

  val validName = Name(
    testRegisterIndividualWithIdSuccessResponse.firstName,
    testRegisterIndividualWithIdSuccessResponse.lastName
  )

  val orgUkBusinessResponse = RegisterOrganisationWithIdResponse(
    safeId = "testSafeId",
    code = Some("0000"),
    organisationName = "Agent ABC Ltd",
    address = AddressRegistrationResponse(
      addressLine1 = "2 High Street",
      addressLine2 = Some("Birmingham"),
      addressLine3 = None,
      addressLine4 = None,
      postalCode = Some("B23 2AZ"),
      countryCode = "GB",
      countryName = None
    )
  )

  val testRegisterIndividualWithoutIdRequest = RegisterIndividualWithoutIdRequest(
    firstName = "John",
    lastName = "Doe",
    dateOfBirth = "1990-01-01",
    address = testAddressDetails,
    contactDetails = testContactDetails
  )

  val testRegisterWithoutIdResponse = RegisterWithoutIdResponse(
    safeId = testSafeId
  )

  val organisationUserAnswersUtr: UserAnswers = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(testUtrString))
    .success
    .value
    .set(WhatIsTheNameOfYourBusinessPage, orgUkBusinessResponse.organisationName)
    .success
    .value
    .set(RegistrationTypePage, testOrgType)
    .success
    .value

  val individualDetailsUtrUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("5234567890"))
    .success
    .value
    .set(WhatIsYourNamePage, Name("ST firstName", "ST lastName"))
    .success
    .value

  val utrNotPresentInIndividualDetailsUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(WhatIsYourNamePage, Name("ST firstName", "ST lastName"))
    .success
    .value

  val nameNotPresentIndividualDetailsUserAnswers: UserAnswers = UserAnswers(userAnswersId)
    .set(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference("5234567890"))
    .success
    .value

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockConnector)
  }

  "RegistrationService" - {
    "getBusinessWithUtr when the user has a ct enrolment should" - {
      "return business details when the connector finds a match" in {
        val expectedRequest = RegOrgWithIdCTAutoMatchRequest(
          requiresNameMatch = false,
          IDNumber = testUtrString,
          IDType = "UTR"
        )

        when(mockConnector.organisationWithUtrCTAutoMatch(eqTo(expectedRequest))(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](orgUkBusinessResponse))

        val result = testService
          .getBusinessWithUtr(emptyUserAnswers.copy(isCtAutoMatched = true), testUtrString)
          .futureValue

        result mustBe Right(
          BusinessDetails(
            orgUkBusinessResponse.organisationName,
            orgUkBusinessResponse.address,
            orgUkBusinessResponse.safeId
          )
        )
        verify(mockConnector).organisationWithUtrCTAutoMatch(eqTo(expectedRequest))(any())
      }

      "return a not found error when the connector does not find a match" in {
        when(mockConnector.organisationWithUtrCTAutoMatch(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterOrganisationWithIdResponse](NotFoundError))

        val result = testService
          .getBusinessWithUtr(emptyUserAnswers.copy(isCtAutoMatched = true), testUtrString)
          .futureValue

        result mustBe Left(NotFoundError)
      }
    }

    "getBusinessWithUtr when the user is on the manual entry journey" - {
      "return business details when UserAnswers is complete and connector finds a match" in {
        val expectedRequest = RegOrgWithIdNonAutoMatchRequest(
          requiresNameMatch = true,
          IDNumber = testUtrString,
          IDType = "UTR",
          organisationName = orgUkBusinessResponse.organisationName,
          organisationType = testOrgType.code
        )

        when(mockConnector.organisationWithUtrNonAutoMatch(eqTo(expectedRequest))(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](orgUkBusinessResponse))

        val result = testService.getBusinessWithUtr(organisationUserAnswersUtr, testUtrString).futureValue

        result mustBe Right(
          BusinessDetails(
            orgUkBusinessResponse.organisationName,
            orgUkBusinessResponse.address,
            orgUkBusinessResponse.safeId
          )
        )
        verify(mockConnector).organisationWithUtrNonAutoMatch(eqTo(expectedRequest))(any())
      }

      "return a data error when UserAnswers is missing data" in {
        val incompleteUserAnswers = UserAnswers(userAnswersId)
        val result                = testService.getBusinessWithUtr(incompleteUserAnswers, testUtrString).futureValue

        result mustBe Left(DataError)
        verify(mockConnector, never()).organisationWithUtrNonAutoMatch(any())(any())
      }

      "return an internal server error when one is returned by the connector" in {
        when(mockConnector.organisationWithUtrNonAutoMatch(any())(any()))
          .thenReturn(EitherT.leftT[Future, RegisterOrganisationWithIdResponse](InternalServerError))
        val result =
          testService.getBusinessWithUtr(organisationUserAnswersUtr, testUtrString).futureValue

        result mustBe Left(InternalServerError)
      }
    }

    "getIndividualByNino method should" - {
      "successfully return an individual's details when the connector returns them successfully" in {
        when(mockConnector.individualWithNino(any[RegisterIndividualWithNinoRequest])(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testRegisterIndividualWithIdSuccessResponse))

        val result = testService
          .getIndividualByNino(ninoOkFullIndividualResponse, validName, validBirthDate)
          .futureValue

        result mustBe Right(
          IndividualDetails(
            safeId = "testSafeId",
            firstName = "Floriane",
            lastName = "Yammel",
            middleName = Some("Exie"),
            address = testAddress
          )
        )
      }

      "return a not found error when the connector could not get a record match for this user" in {
        when(mockConnector.individualWithNino(any[RegisterIndividualWithNinoRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](NotFoundError))

        val result = testService.getIndividualByNino(ninoNotFound, validName, validBirthDate).futureValue
        result mustBe Left(NotFoundError)
      }
    }

    "getIndividualByUtr method should" - {
      "return individual details when UserAnswers is complete and connector finds a match" in {
        val expectedRequest = RegisterIndividualWithUtrRequest(
          requiresNameMatch = true,
          IDNumber = "5234567890",
          IDType = "UTR",
          firstName = "ST firstName",
          lastName = "ST lastName"
        )
        when(mockConnector.individualWithUtr(eqTo(expectedRequest))(any()))
          .thenReturn(EitherT.rightT[Future, ApiError](testRegisterIndividualWithIdSuccessResponse))

        val result = testService.getIndividualByUtr(individualDetailsUtrUserAnswers).futureValue

        result mustBe Right(
          IndividualDetails(
            safeId = "testSafeId",
            firstName = "Floriane",
            lastName = "Yammel",
            middleName = Some("Exie"),
            address = testAddress
          )
        )
        verify(mockConnector).individualWithUtr(eqTo(expectedRequest))(any())
      }

      "return a not found error when the connector could not get a record match for this user" in {
        when(mockConnector.individualWithUtr(any[RegisterIndividualWithUtrRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](NotFoundError))

        val result = testService.getIndividualByUtr(individualDetailsUtrUserAnswers).futureValue
        result mustBe Left(NotFoundError)
      }

      "return a data error when the UserAnswers does not contain a Utr" in {
        val result = testService.getIndividualByUtr(utrNotPresentInIndividualDetailsUserAnswers).futureValue

        result mustBe Left(DataError)
        verify(mockConnector, never()).individualWithUtr(any())(any())
      }

      "return None when the UserAnswers does not contain a Name" in {
        val result = testService.getIndividualByUtr(nameNotPresentIndividualDetailsUserAnswers).futureValue

        result mustBe Left(DataError)
        verify(mockConnector, never()).individualWithUtr(any())(any())
      }

      "return an internal server error when the connector returns one" in {
        when(mockConnector.individualWithUtr(any[RegisterIndividualWithUtrRequest])(any()))
          .thenReturn(EitherT.leftT[Future, RegisterIndividualWithIdResponse](InternalServerError))

        val result = testService.getIndividualByUtr(individualDetailsUtrUserAnswers).futureValue

        result mustBe Left(InternalServerError)
      }
    }
    "registerForWithoutIdJourneys" - {
      "must return the input for org with utr journeys" in {
        val testUserAnswers = emptyUserAnswers.copy(journeyType = Some(OrgWithUtr))
        val result          = testService.registerForWithoutIdJourneys(testUserAnswers).value.futureValue

        result mustBe Right(testUserAnswers)
        verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
      }
      "must return the input for ind with utr journeys" in {
        val testUserAnswers = emptyUserAnswers.copy(journeyType = Some(IndWithUtr))
        val result          = testService.registerForWithoutIdJourneys(testUserAnswers).value.futureValue

        result mustBe Right(testUserAnswers)
        verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
      }
      "must return the input for ind with nino journeys" in {
        val testUserAnswers = emptyUserAnswers.copy(journeyType = Some(IndWithNino))
        val result          = testService.registerForWithoutIdJourneys(testUserAnswers).value.futureValue

        result mustBe Right(testUserAnswers)
        verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
      }
      "must return an error when journey type is None" in {
        val result = testService.registerForWithoutIdJourneys(emptyUserAnswers).value.futureValue

        result mustBe Left(DataError)
        verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
      }
      "when journey type is organisation without id" - {
        val testFullUserAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(OrgWithoutId))
            .withPage(OrgWithoutIdBusinessNamePage, "TestBiz")
            .withPage(OrganisationBusinessAddressPage, testBusinessAddress)
            .withPage(FirstContactEmailPage, testEmail)
            .withPage(FirstContactPhonePage, true)
            .withPage(FirstContactPhoneNumberPage, testPhone)

        "must return user answers with new safe id when the connector returns a success" in {
          val expectedUserAnswers = testFullUserAnswers.copy(safeId = Some(SafeId(testSafeId)))
          val expectedRequest     = RegisterOrganisationWithoutIdRequest(
            organisationName = "TestBiz",
            address = testAddressDetails,
            contactDetails = testContactDetails
          )

          when(mockConnector.registerOrganisationWithoutId(eqTo(expectedRequest))(any()))
            .thenReturn(EitherT.rightT[Future, CarfError](testRegisterWithoutIdResponse))

          val result = testService.registerForWithoutIdJourneys(testFullUserAnswers).value.futureValue

          result mustBe Right(expectedUserAnswers)
          verify(mockConnector).registerOrganisationWithoutId(eqTo(expectedRequest))(any())
        }
        "must return user answers with new safe id when the connector returns a success and have phone is false" in {
          val baseUserAnswersWithoutPhone = testFullUserAnswers.withPage(FirstContactPhonePage, false)

          val expectedUserAnswers = baseUserAnswersWithoutPhone
            .copy(safeId = Some(SafeId(testSafeId)))

          val expectedRequest = RegisterOrganisationWithoutIdRequest(
            organisationName = "TestBiz",
            address = testAddressDetails,
            contactDetails = ContactDetails(testEmail, None)
          )

          when(mockConnector.registerOrganisationWithoutId(eqTo(expectedRequest))(any()))
            .thenReturn(EitherT.rightT[Future, CarfError](testRegisterWithoutIdResponse))

          val result = testService.registerForWithoutIdJourneys(baseUserAnswersWithoutPhone).value.futureValue

          result mustBe Right(expectedUserAnswers)
          verify(mockConnector).registerOrganisationWithoutId(eqTo(expectedRequest))(any())
        }
        "must return an error when the connector returns an error" in {
          when(mockConnector.registerOrganisationWithoutId(any())(any()))
            .thenReturn(EitherT.leftT[Future, RegisterWithoutIdResponse](InternalServerError))

          val result = testService.registerForWithoutIdJourneys(testFullUserAnswers).value.futureValue

          result mustBe Left(InternalServerError)
          verify(mockConnector).registerOrganisationWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as organisation name is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswers.withoutPage(OrgWithoutIdBusinessNamePage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as organisation address is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswers.withoutPage(OrganisationBusinessAddressPage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as organisation email address is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswers.withoutPage(FirstContactEmailPage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as organisation have phone is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswers.withoutPage(FirstContactEmailPage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as organisation phone is missing when have phone is true" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswers.withoutPage(FirstContactPhoneNumberPage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerOrganisationWithoutId(any())(any())
        }
      }

      "when journey type is individual without id" - {
        val testFullUserAnswersUk: UserAnswers =
          emptyUserAnswers
            .copy(journeyType = Some(IndWithoutId))
            .withPage(IndWithoutNinoNamePage, Name("Timmy", "Timmington"))
            .withPage(IndWithoutIdDateOfBirthPage, testDob)
            .withPage(WhereDoYouLivePage, true)
            .withPage(IndWithoutIdUkAddressInUserAnswers, testAddressUk)
            .withPage(IndividualEmailPage, testEmail)
            .withPage(IndividualHavePhonePage, true)
            .withPage(IndividualPhoneNumberPage, testPhone)

        val testFullUserAnswersNonUk: UserAnswers =
          testFullUserAnswersUk
            .withoutPage(IndWithoutIdUkAddressInUserAnswers)
            .withPage(IndWithoutIdAddressNonUkPage, testIndWithoutIdAddressNonUk)
            .withPage(WhereDoYouLivePage, false)

        "must return user answers with new safe id when the connector returns a success for uk individual" in {
          val expectedUserAnswers = testFullUserAnswersUk.copy(safeId = Some(SafeId(testSafeId)))
          val expectedRequest     = RegisterIndividualWithoutIdRequest(
            firstName = "Timmy",
            lastName = "Timmington",
            dateOfBirth = testDob.toString,
            address = testAddressDetailsUk,
            contactDetails = testContactDetails
          )

          when(mockConnector.registerIndividualWithoutId(eqTo(expectedRequest))(any()))
            .thenReturn(EitherT.rightT[Future, CarfError](testRegisterWithoutIdResponse))

          val result = testService.registerForWithoutIdJourneys(testFullUserAnswersUk).value.futureValue

          result mustBe Right(expectedUserAnswers)
          verify(mockConnector).registerIndividualWithoutId(eqTo(expectedRequest))(any())
        }
        "must return user answers with new safe id when the connector returns a success and have phone is false" in {
          val testUserAnswers =
            testFullUserAnswersUk
              .withPage(IndividualHavePhonePage, false)

          val expectedUserAnswersWithSafeId =
            testUserAnswers
              .copy(safeId = Some(SafeId(testSafeId)))

          val expectedRequest = RegisterIndividualWithoutIdRequest(
            firstName = "Timmy",
            lastName = "Timmington",
            dateOfBirth = testDob.toString,
            address = testAddressDetailsUk,
            contactDetails = ContactDetails(testEmail, None)
          )

          when(mockConnector.registerIndividualWithoutId(eqTo(expectedRequest))(any()))
            .thenReturn(EitherT.rightT[Future, CarfError](testRegisterWithoutIdResponse))

          val result = testService.registerForWithoutIdJourneys(testUserAnswers).value.futureValue

          result mustBe Right(expectedUserAnswersWithSafeId)
          verify(mockConnector).registerIndividualWithoutId(eqTo(expectedRequest))(any())
        }
        "must return user answers with new safe id when the connector returns a success for NON-uk individual" in {
          val expectedUserAnswersWithSafeId = testFullUserAnswersNonUk.copy(safeId = Some(SafeId(testSafeId)))

          val expectedRequest = RegisterIndividualWithoutIdRequest(
            firstName = "Timmy",
            lastName = "Timmington",
            dateOfBirth = testDob.toString,
            address = testAddressDetailsIndNonUk,
            contactDetails = ContactDetails(testEmail, Some(testPhone))
          )

          when(mockConnector.registerIndividualWithoutId(eqTo(expectedRequest))(any()))
            .thenReturn(EitherT.rightT[Future, CarfError](testRegisterWithoutIdResponse))

          val result = testService.registerForWithoutIdJourneys(testFullUserAnswersNonUk).value.futureValue

          result mustBe Right(expectedUserAnswersWithSafeId)
          verify(mockConnector).registerIndividualWithoutId(eqTo(expectedRequest))(any())
        }
        "must return an error when the connector returns an error" in {
          when(mockConnector.registerIndividualWithoutId(any())(any()))
            .thenReturn(EitherT.leftT[Future, RegisterWithoutIdResponse](InternalServerError))

          val result = testService.registerForWithoutIdJourneys(testFullUserAnswersUk).value.futureValue

          result mustBe Left(InternalServerError)
          verify(mockConnector).registerIndividualWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as name is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswersUk.withoutPage(IndWithoutNinoNamePage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as uk individual address is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswersUk.withoutPage(IndWithoutIdUkAddressInUserAnswers))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as NON-uk individual address is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswersNonUk.withoutPage(IndWithoutIdAddressNonUkPage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as individual email address is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswersUk.withoutPage(IndividualEmailPage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as individual have phone is missing" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswersUk.withoutPage(IndividualHavePhonePage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
        }
        "must return an error when the request cannot be constructed as individual phone is missing when have phone is true" in {
          val result = testService
            .registerForWithoutIdJourneys(testFullUserAnswersUk.withoutPage(IndividualPhoneNumberPage))
            .value
            .futureValue

          result mustBe Left(DataError)
          verify(mockConnector, never()).registerIndividualWithoutId(any())(any())
        }
      }
    }
  }
}
