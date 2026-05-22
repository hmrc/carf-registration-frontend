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

package services

import base.SpecBase
import models.JourneyType.*
import models.RegistrationType.*
import models.audit.*
import models.countries.{Country, CountryUk, UnitedKingdom}
import models.error.ApiError.InternalServerError
import models.error.DataError
import models.responses.AddressRegistrationResponse
import models.*
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.individual.*
import pages.individualWithoutId.{IndWithoutIdAddressNonUkPage, IndWithoutIdDateOfBirthPage, IndWithoutIdUkAddressInUserAnswers, IndWithoutNinoNamePage}
import pages.orgWithoutId.{HaveTradingNamePage, OrgWithoutIdBusinessNamePage, OrganisationBusinessAddressPage, TradingNamePage}
import pages.organisation.*
import pages.{AddressUPRNUserAnswers, IsThisYourBusinessPage, RegisteredAddressInUkPage, WhereDoYouLivePage}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.*
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import java.time.LocalDate
import scala.concurrent.Future

class AuditServiceSpec extends SpecBase with MockitoSugar {

  private val mockAuditConnector = mock[AuditConnector]
  private val service            = new AuditService(mockAuditConnector)

  override implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach(): Unit = {
    reset(mockAuditConnector)
    super.beforeEach()
  }

  private final val orgAffinityGroup = Organisation
  private final val indAffinityGroup = AffinityGroup.Individual

  "AuditService" - {
    "registration audit event" - {
      "should return success for withUtrJourney" in {

        val regType     = SoleTrader
        val utr         = "testUtr"
        val name        = Name("firstname", "lastname")
        val userAnswers = emptyUserAnswers
          .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(utr))
          .withPage(RegisteredAddressInUkPage, true)
          .withPage(WhatIsYourNamePage, name)
          .withPage(
            IsThisYourBusinessPage,
            IsThisYourBusinessPageDetails(
              businessDetails = BusinessDetails(
                "Test Business",
                AddressRegistrationResponse("Test Line 1", None, None, None, None, "GB", None),
                safeId = testSafeId
              ),
              Some(true)
            )
          )
          .withPage(RegistrationTypePage, regType)
          .withPage(HaveUTRPage, true)

        val expectedExtendedAudit = RegistrationAuditEvent(
          affinityGroup = indAffinityGroup,
          registeredAs = regType,
          registeredUkAddress = Some(true),
          hasUtr = Some(true),
          hasNINO = None,
          withUtrJourney = Some(
            UtrJourneyAuditEvent(
              utr,
              name.firstName,
              name.lastName,
              true
            )
          ),
          organisationWithIdJourney = None,
          organisationWithoutIdJourney = None,
          withNinoJourney = None,
          individualWithoutIdJourney = None,
          individualContactDetails = None,
          organisationContactDetails = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditRegistration(userAnswers, IndWithUtr, indAffinityGroup).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-registration-frontend" && event.auditType == "Registration"
              && event.detail == Json.toJson(expectedExtendedAudit)
          )
        )(any(), any())
      }

      "should return Internal server error when Disabled is returned by audit connector" in {

        val regType     = LimitedCompany
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, regType)

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Disabled))

        val result = service.auditRegistration(userAnswers, IndWithUtr, indAffinityGroup).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return Internal server error when Failure is returned by audit connector" in {

        val regType     = LimitedCompany
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, regType)

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Failure))

        val result = service.auditRegistration(userAnswers, IndWithUtr, indAffinityGroup).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return Internal server error when call to audit connector's future fails" in {

        val regType     = LimitedCompany
        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, regType)

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.failed(new Exception("whoops")))

        val result = service.auditRegistration(userAnswers, IndWithUtr, indAffinityGroup).value.futureValue

        result mustBe Left(InternalServerError)

      }

      "should return DataError when RegistrationTypePage is missing from user answers" in {

        val userAnswers = emptyUserAnswers

        val result = service.auditRegistration(userAnswers, IndWithUtr, indAffinityGroup).value.futureValue

        result mustBe Left(DataError)
      }

      "should return success for organisationWithIdJourney" in {

        val regType     = LimitedCompany
        val utr         = "testUtr"
        val name        = "Arsenal FC"
        val userAnswers = emptyUserAnswers
          .withPage(UniqueTaxpayerReferenceInUserAnswers, UniqueTaxpayerReference(utr))
          .withPage(RegisteredAddressInUkPage, true)
          .withPage(WhatIsTheNameOfYourBusinessPage, name)
          .withPage(
            IsThisYourBusinessPage,
            IsThisYourBusinessPageDetails(
              businessDetails = BusinessDetails(
                "Test Business",
                AddressRegistrationResponse("Test Line 1", None, None, None, None, "GB", None),
                safeId = testSafeId
              ),
              Some(true)
            )
          )
          .withPage(RegistrationTypePage, regType)
          .withPage(HaveUTRPage, true)
          .withPage(FirstContactNamePage, name)
          .withPage(FirstContactEmailPage, testEmail)
          .withPage(FirstContactPhonePage, false)
          .withPage(OrganisationHaveSecondContactPage, false)

        val expectedExtendedAudit = RegistrationAuditEvent(
          affinityGroup = orgAffinityGroup,
          registeredAs = regType,
          registeredUkAddress = Some(true),
          hasUtr = Some(true),
          hasNINO = None,
          withUtrJourney = None,
          organisationWithIdJourney = Some(OrganisationWithIdJourney(utr, name, true)),
          organisationWithoutIdJourney = None,
          withNinoJourney = None,
          individualWithoutIdJourney = None,
          individualContactDetails = None,
          organisationContactDetails = Some(
            OrganisationContactDetails(name, testEmail, false, None, false, None, None, None, None)
          )
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditRegistration(userAnswers, OrgWithUtr, orgAffinityGroup).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-registration-frontend" && event.auditType == "Registration"
              && event.detail == Json.toJson(expectedExtendedAudit)
          )
        )(any(), any())
      }

      "should return success for organisationWithoutIdJourney" in {

        val regType                     = LimitedCompany
        val name                        = "Arsenal FC"
        val organisationBusinessAddress =
          OrganisationBusinessAddress("Test Line 1", None, "Town", None, None, UnitedKingdom)

        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, regType)
          .withPage(HaveUTRPage, false)
          .withPage(RegisteredAddressInUkPage, true)
          .withPage(HaveTradingNamePage, true)
          .withPage(TradingNamePage, name)
          .withPage(OrgWithoutIdBusinessNamePage, name)
          .withPage(
            OrganisationBusinessAddressPage,
            organisationBusinessAddress
          )

        val expectedExtendedAudit = RegistrationAuditEvent(
          affinityGroup = orgAffinityGroup,
          registeredAs = regType,
          registeredUkAddress = Some(true),
          hasUtr = Some(false),
          hasNINO = None,
          withUtrJourney = None,
          organisationWithIdJourney = None,
          organisationWithoutIdJourney = Some(
            OrganisationWithoutIdJourney(
              name,
              true,
              Some(name),
              organisationBusinessAddress.addressLine1,
              organisationBusinessAddress.addressLine2,
              organisationBusinessAddress.townOrCity,
              organisationBusinessAddress.region,
              organisationBusinessAddress.postcode,
              organisationBusinessAddress.country.description
            )
          ),
          withNinoJourney = None,
          individualWithoutIdJourney = None,
          individualContactDetails = None,
          organisationContactDetails = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditRegistration(userAnswers, OrgWithoutId, orgAffinityGroup).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-registration-frontend" && event.auditType == "Registration"
              && event.detail == Json.toJson(expectedExtendedAudit)
          )
        )(any(), any())
      }

      "should return success for withNinoJourney" in {

        val regType = Individual
        val name    = Name("firstname", "lastname")
        val nino    = "nino"

        val userAnswers = emptyUserAnswers
          .withPage(HaveNiNumberPage, true)
          .withPage(NiNumberPage, nino)
          .withPage(WhatIsYourNameIndividualPage, name)
          .withPage(RegisterDateOfBirthPage, LocalDate.of(2026, 5, 19))
          .withPage(RegistrationTypePage, Individual)

        val expectedExtendedAudit = RegistrationAuditEvent(
          affinityGroup = indAffinityGroup,
          registeredAs = regType,
          registeredUkAddress = None,
          hasUtr = None,
          hasNINO = Some(true),
          withUtrJourney = None,
          organisationWithIdJourney = None,
          organisationWithoutIdJourney = None,
          withNinoJourney = Some(
            WithNinoJourney(
              nino,
              name.firstName,
              name.lastName,
              "2026-05-19"
            )
          ),
          individualWithoutIdJourney = None,
          individualContactDetails = None,
          organisationContactDetails = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditRegistration(userAnswers, IndWithNino, indAffinityGroup).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-registration-frontend" && event.auditType == "Registration"
              && event.detail == Json.toJson(expectedExtendedAudit)
          )
        )(any(), any())
      }

      "should return success for individualWithoutIdJourney (UK)" in {

        val regType   = Individual
        val name      = Name("Arteta", "lastname")
        val addressUk = AddressUk("Test Line 1", None, None, "town", "N7 7AJ", CountryUk("UK", "United Kingdom"))
        val uprn      = 123456789

        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, Individual)
          .withPage(HaveNiNumberPage, false)
          .withPage(IndWithoutIdUkAddressInUserAnswers, addressUk)
          .withPage(IndWithoutNinoNamePage, name)
          .withPage(IndWithoutIdDateOfBirthPage, LocalDate.of(2026, 5, 19))
          .withPage(WhereDoYouLivePage, true)
          .withPage(AddressUPRNUserAnswers, uprn)
          .withPage(IndividualEmailPage, testEmail)
          .withPage(IndividualHavePhonePage, false)

        val expectedExtendedAudit = RegistrationAuditEvent(
          affinityGroup = indAffinityGroup,
          registeredAs = regType,
          registeredUkAddress = Some(true),
          hasUtr = None,
          hasNINO = Some(false),
          withUtrJourney = None,
          organisationWithIdJourney = None,
          organisationWithoutIdJourney = None,
          withNinoJourney = None,
          individualWithoutIdJourney = Some(
            IndividualWithoutIdJourney(
              name.firstName,
              name.lastName,
              "2026-05-19",
              true,
              false,
              None,
              None,
              Some(uprn.toString),
              addressUk.addressLine1,
              addressUk.addressLine2,
              addressUk.addressLine3,
              addressUk.townOrCity,
              None,
              Some(addressUk.postCode),
              addressUk.countryUk.name
            )
          ),
          individualContactDetails = Some(IndividualContactDetails(testEmail, false, None)),
          organisationContactDetails = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditRegistration(userAnswers, IndWithoutId, indAffinityGroup).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-registration-frontend" && event.auditType == "Registration"
              && event.detail == Json.toJson(expectedExtendedAudit)
          )
        )(any(), any())
      }

      "should return success for individualWithoutIdJourney (Non-UK)" in {

        val regType      = Individual
        val name         = Name("firstname", "lastname")
        val addressNonUk =
          IndWithoutIdAddressNonUk("Test Line 1", None, "Islington", Some("region"), None, Country("FR", "France"))
        val uprn         = 123456789

        val userAnswers = emptyUserAnswers
          .withPage(RegistrationTypePage, Individual)
          .withPage(HaveNiNumberPage, false)
          .withPage(IndWithoutIdAddressNonUkPage, addressNonUk)
          .withPage(IndWithoutNinoNamePage, name)
          .withPage(IndWithoutIdDateOfBirthPage, LocalDate.of(2026, 5, 19))
          .withPage(WhereDoYouLivePage, false)
          .withPage(AddressUPRNUserAnswers, uprn)

        val expectedExtendedAudit = RegistrationAuditEvent(
          affinityGroup = indAffinityGroup,
          registeredAs = regType,
          registeredUkAddress = Some(false),
          hasUtr = None,
          hasNINO = Some(false),
          withUtrJourney = None,
          organisationWithIdJourney = None,
          organisationWithoutIdJourney = None,
          withNinoJourney = None,
          individualWithoutIdJourney = Some(
            IndividualWithoutIdJourney(
              name.firstName,
              name.lastName,
              "2026-05-19",
              false,
              false,
              None,
              None,
              Some(uprn.toString),
              addressNonUk.addressLine1,
              addressNonUk.addressLine2,
              None,
              addressNonUk.townOrCity,
              addressNonUk.region,
              addressNonUk.postcode,
              addressNonUk.country.description
            )
          ),
          individualContactDetails = None,
          organisationContactDetails = None
        )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(Success))

        val result = service.auditRegistration(userAnswers, IndWithoutId, indAffinityGroup).value.futureValue

        result mustBe Right(())

        verify(mockAuditConnector, times(1)).sendExtendedEvent(
          argThat(event =>
            event.auditSource == "carf-registration-frontend" && event.auditType == "Registration"
              && event.detail == Json.toJson(expectedExtendedAudit)
          )
        )(any(), any())
      }
    }
  }
}
