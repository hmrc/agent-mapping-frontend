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

package uk.gov.hmrc.agentmappingfrontend.controllers

import play.api.Configuration
import play.api.Environment
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentmappingfrontend.auth.AuthActions
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmappingfrontend.connectors.MappingConnector
import uk.gov.hmrc.agentmappingfrontend.model.LegacyAgentEnrolmentType
import uk.gov.hmrc.agentmappingfrontend.model.MappingStatus
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.mongo.lock.TimePeriodLockService
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.*

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class MappingCheckController @Inject() (
  mappingConnector: MappingConnector,
  mcc: MessagesControllerComponents,
  mongoLockRepository: MongoLockRepository,
  val authConnector: AuthConnector,
  val config: Configuration,
  val env: Environment
)(implicit
  val ec: ExecutionContext,
  val appConfig: AppConfig
)
extends FrontendController(mcc)
with I18nSupport
with AuthActions:

  def checkMapping(
    enrolmentType: LegacyAgentEnrolmentType,
    returnUrl: RedirectUrl
  ): Action[AnyContent] =
    Action.async:
      implicit request =>
        val validatedReturnUrl = returnUrl.get(appConfig.redirectUrlPolicy).url
        lazy val sessionMarker = s"mapping-checked-${enrolmentType.serviceKey}" -> "true"
        withAuthorisedLegacyAgent(enrolmentType, validatedReturnUrl): enrolment =>
          mappingConnector
            .getMappingStatus(enrolment.identifiers.head.value, enrolmentType)
            .flatMap:
              case MappingStatus.Mapped => Future.successful(Redirect(validatedReturnUrl).addingToSession(sessionMarker))
              case MappingStatus.NotMappedMandated(continueUrl) => Future.successful(Redirect(continueUrl, Map("returnUrl" -> Seq(validatedReturnUrl))))
              case MappingStatus.NotMappedNotMandated(continueUrl) =>
                isUserAlreadyNotified(enrolment, enrolmentType).map:
                  case true => Redirect(validatedReturnUrl).addingToSession(sessionMarker)
                  case false => Redirect(continueUrl, Map("returnUrl" -> Seq(validatedReturnUrl)))
              case MappingStatus.Suspended(continueUrl) => Future.successful(Redirect(continueUrl))

  private def isUserAlreadyNotified(
    enrolment: Enrolment,
    enrolmentType: LegacyAgentEnrolmentType
  ): Future[Boolean] =
    TimePeriodLockService(
      mongoLockRepository,
      s"$enrolmentType-${enrolment.identifiers.head.value}",
      1.day
    ).withRenewedLock(Future.unit).map:
      case Some(_) => false
      case None => true
