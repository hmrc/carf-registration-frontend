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

package testUtils

import base.SpecBase
import models.UserAnswers
import pages.changeContactDetails.*

trait ChangeDetailsTestData extends SpecBase {
  val userAnswersWithoutHavePhone: UserAnswers =
    emptyUserAnswers
      .copy(displaySubscriptionResponse = Some(testOrganisationDisplaySubscriptionResponse))
      .withPage(ChangeDetailsOrgFirstNamePage, testName)
      .withPage(ChangeDetailsOrgFirstEmailPage, testEmail)
      .withPage(ChangeDetailsOrgHaveSecondContactPage, false)

  val userAnswersPhoneMissing: UserAnswers = emptyUserAnswers
    .copy(displaySubscriptionResponse = Some(testOrganisationDisplaySubscriptionResponse))
    .withPage(ChangeDetailsOrgFirstNamePage, testName)
    .withPage(ChangeDetailsOrgFirstEmailPage, testEmail)
    .withPage(ChangeDetailsOrgFirstHavePhonePage, true)
    .withPage(ChangeDetailsOrgHaveSecondContactPage, false)

  val fullUserAnswers: UserAnswers = emptyUserAnswers
    .copy(displaySubscriptionResponse = Some(testOrganisationDisplaySubscriptionResponse))
    .withPage(ChangeDetailsOrgFirstNamePage, testName)
    .withPage(ChangeDetailsOrgFirstEmailPage, testEmail)
    .withPage(ChangeDetailsOrgFirstHavePhonePage, true)
    .withPage(ChangeDetailsOrgFirstPhoneNumberPage, testPhone)
    .withPage(ChangeDetailsOrgHaveSecondContactPage, false)

  val userAnswersWithoutDisplay: UserAnswers = emptyUserAnswers
    .withPage(ChangeDetailsOrgFirstNamePage, testName)
    .withPage(ChangeDetailsOrgFirstEmailPage, testEmail)
    .withPage(ChangeDetailsOrgFirstHavePhonePage, true)
    .withPage(ChangeDetailsOrgFirstPhoneNumberPage, testPhone)
    .withPage(ChangeDetailsOrgHaveSecondContactPage, false)

  val fullFirstContactUserAnswersWithSecondContactMissing: UserAnswers = emptyUserAnswers
    .copy(displaySubscriptionResponse = Some(testOrganisationDisplaySubscriptionResponse))
    .withPage(ChangeDetailsOrgFirstNamePage, testName)
    .withPage(ChangeDetailsOrgFirstEmailPage, testEmail)
    .withPage(ChangeDetailsOrgFirstHavePhonePage, true)
    .withPage(ChangeDetailsOrgFirstPhoneNumberPage, testPhone)

  val userAnswersNoPhone: UserAnswers = emptyUserAnswers
    .copy(displaySubscriptionResponse = Some(testOrganisationDisplaySubscriptionResponse))
    .withPage(ChangeDetailsOrgFirstNamePage, testName)
    .withPage(ChangeDetailsOrgFirstEmailPage, testEmail)
    .withPage(ChangeDetailsOrgFirstHavePhonePage, false)
    .withPage(ChangeDetailsOrgHaveSecondContactPage, false)

  val userAnswersNameMissing: UserAnswers = emptyUserAnswers
    .copy(displaySubscriptionResponse = Some(testOrganisationDisplaySubscriptionResponse))
    .withPage(ChangeDetailsOrgFirstEmailPage, testEmail)
    .withPage(ChangeDetailsOrgFirstHavePhonePage, true)
    .withPage(ChangeDetailsOrgFirstPhoneNumberPage, testPhone)
    .withPage(ChangeDetailsOrgHaveSecondContactPage, false)

  val userAnswersWithoutSecondContact: UserAnswers =
    emptyUserAnswers
      .withPage(ChangeDetailsOrgHaveSecondContactPage, false)

  val userAnswersWithSecondContact: UserAnswers =
    emptyUserAnswers
      .copy(displaySubscriptionResponse = Some(testOrganisationDisplaySubscriptionResponse))
      .withPage(ChangeDetailsOrgFirstNamePage, testName)
      .withPage(ChangeDetailsOrgFirstEmailPage, testEmail)
      .withPage(ChangeDetailsOrgFirstHavePhonePage, true)
      .withPage(ChangeDetailsOrgFirstPhoneNumberPage, testPhone)
      .withPage(ChangeDetailsOrgHaveSecondContactPage, true)

  val userAnswersSecondContactWithoutHavePhone: UserAnswers =
    userAnswersWithSecondContact
      .withPage(ChangeDetailsOrgSecondNamePage, testName)
      .withPage(ChangeDetailsOrgSecondEmailPage, testEmail)

  val userAnswersSecondPhoneMissing: UserAnswers = userAnswersWithSecondContact
    .withPage(ChangeDetailsOrgSecondNamePage, testName)
    .withPage(ChangeDetailsOrgSecondEmailPage, testEmail)
    .withPage(ChangeDetailsOrgSecondHavePhonePage, true)

  val fullSecondContactUserAnswers: UserAnswers = userAnswersWithSecondContact
    .withPage(ChangeDetailsOrgSecondNamePage, testName)
    .withPage(ChangeDetailsOrgSecondEmailPage, testEmail)
    .withPage(ChangeDetailsOrgSecondHavePhonePage, true)
    .withPage(ChangeDetailsOrgSecondPhoneNumberPage, testPhone)

  val userAnswersSecondContactNoPhone: UserAnswers = userAnswersWithSecondContact
    .withPage(ChangeDetailsOrgSecondNamePage, testName)
    .withPage(ChangeDetailsOrgSecondEmailPage, testEmail)
    .withPage(ChangeDetailsOrgSecondHavePhonePage, false)

  val userAnswersSecondContactNameMissing: UserAnswers = userAnswersWithSecondContact
    .withPage(ChangeDetailsOrgSecondEmailPage, testEmail)
    .withPage(ChangeDetailsOrgSecondHavePhonePage, true)
    .withPage(ChangeDetailsOrgSecondPhoneNumberPage, testPhone)

}
