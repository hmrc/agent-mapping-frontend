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

package uk.gov.hmrc.agentmappingfrontend.connectors

import com.codahale.metrics.MetricRegistry
import com.github.blemale.scaffeine.Scaffeine
import com.kenshoo.play.metrics.Metrics
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentmappingfrontend.config.AppConfig
import uk.gov.hmrc.agentmtdidentifiers.model._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpClient, _}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AgentClientAuthorisationConnector @Inject()(http: HttpClient, metrics: Metrics)(implicit val appConfig: AppConfig)
    extends HttpAPIMonitor with Logging {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  private val suspensionCache: com.github.blemale.scaffeine.Cache[Arn, SuspensionDetails] =
    Scaffeine()
      .recordStats()
      .expireAfterWrite(appConfig.suspensionCacheDuration)
      .maximumSize(100000)
      .build[Arn, SuspensionDetails]()

  val baseUrl: URL = new URL(appConfig.agentClientAuthorisationBaseUrl)

  // This call is cached as we are doing this check on almost every page
  def getSuspensionDetails(arn: Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[SuspensionDetails] =
    suspensionCache.getIfPresent(arn).map(Future.successful).getOrElse {
      monitor(s"ConsumedAPI-Get-AgencySuspensionDetails-GET") {
        http
          .GET[HttpResponse](s"$baseUrl/agent-client-authorisation/client/suspension-details/${arn.value}")
          .map { response =>
            val suspensionDetails = response.status match {
              case OK         => Json.parse(response.body).as[SuspensionDetails]
              case NO_CONTENT => SuspensionDetails(suspensionStatus = false, None)
              case NOT_FOUND  => throw SuspensionDetailsNotFound("No record found for this agent")
            }
            if (appConfig.suspensionCacheEnabled) suspensionCache.put(arn, suspensionDetails)
            suspensionDetails
          }
      }
    }
}
