/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.model

import java.time.LocalDateTime
import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{JsPath, OFormat}
import play.api.libs.functional.syntax._

final case class SubscriptionJourneyRecord(
  authProviderId: AuthProviderId,
  continueId: Option[String] = None, // once allocated, should not be changed?
  businessDetails: BusinessDetails,
  amlsData: Option[AmlsData] = None,
  userMappings: List[UserMapping] = List.empty,
  mappingComplete: Boolean = false,
  cleanCredsAuthProviderId: Option[AuthProviderId] = None,
  lastModifiedDate: Option[LocalDateTime] = None,
  contactEmailData: Option[ContactEmailData] = None,
  contactTradingNameData: Option[ContactTradingNameData] = None,
  contactTradingAddressData: Option[ContactTradingAddressData] = None,
  contactTelephoneData: Option[ContactTelephoneData] = None,
  verifiedEmails: VerifiedEmails = VerifiedEmails(emails = Set.empty)
)

object SubscriptionJourneyRecord {

  import MongoLocalDateTimeFormat._

  implicit val subscriptionJourneyFormat: OFormat[SubscriptionJourneyRecord] =
    ((JsPath \ "authProviderId").format[AuthProviderId] and
      (JsPath \ "continueId").formatNullable[String] and
      (JsPath \ "businessDetails").format[BusinessDetails] and
      (JsPath \ "amlsData").formatNullable[AmlsData] and
      (JsPath \ "userMappings").format[List[UserMapping]] and
      (JsPath \ "mappingComplete").format[Boolean] and
      (JsPath \ "cleanCredsAuthProviderId").formatNullable[AuthProviderId] and
      (JsPath \ "lastModifiedDate").formatNullable[LocalDateTime] and
      (JsPath \ "contactEmailData").formatNullable[ContactEmailData] and
      (JsPath \ "contactTradingNameData").formatNullable[ContactTradingNameData] and
      (JsPath \ "contactTradingAddressData").formatNullable[ContactTradingAddressData] and
      (JsPath \ "contactTelephoneData").formatNullable[ContactTelephoneData] and
      (JsPath \ "verifiedEmails").formatWithDefault[VerifiedEmails](VerifiedEmails()))(
      SubscriptionJourneyRecord.apply,
      unlift(SubscriptionJourneyRecord.unapply)
    )
}
