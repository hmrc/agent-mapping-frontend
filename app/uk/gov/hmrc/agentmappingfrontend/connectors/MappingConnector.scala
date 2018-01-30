/*
 * Copyright 2018 HM Revenue & Customs
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

import java.net.URL
import javax.inject.{Inject, Named, Singleton}

import play.api.http.Status
import play.api.libs.json.JsValue
import uk.gov.hmrc.agentmappingfrontend.model.{Identifier, Mapping}
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, Utr}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MappingConnector @Inject()(@Named("agent-mapping-baseUrl") baseUrl: URL, httpGet: HttpGet, httpPut: HttpPut, httpDelete: HttpDelete) {

  def createMapping(utr: Utr, arn: Arn, identifier: Identifier)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    httpPut.PUT(createUrl(utr, arn, identifier), "").map{
      r => r.status
    }.recover {
      case e: Upstream4xxResponse if Status.FORBIDDEN.equals(e.upstreamResponseCode) => Status.FORBIDDEN
      case e: Upstream4xxResponse if Status.CONFLICT.equals(e.upstreamResponseCode) => Status.CONFLICT
      case e => throw e
    }
  }

  private def createUrl(utr: Utr, arn: Arn, identifier: Identifier): String = {
    new URL(baseUrl, s"/agent-mapping/mappings/${utr.value}/${arn.value}/$identifier").toString
  }

  private def deleteUrl(arn: Arn): String = {
    new URL(baseUrl, s"/agent-mapping/test-only/mappings/${arn.value}").toString
  }

  private def findUrl(arn: Arn): String = {
    new URL(baseUrl, s"agent-mapping/mappings/${arn.value}").toString
  }

  def find(arn:Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[Mapping]] = {
    httpGet.GET[JsValue](findUrl(arn)).map {
      response => (response \ "mappings").as[Seq[Mapping]]
    } recover {
      case ex: NotFoundException => Seq.empty
      case ex: Throwable => throw new RuntimeException(ex)
    }
  }

  def delete(arn:Arn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Int] = {
    httpDelete.DELETE(deleteUrl(arn)).map {
      r => r.status
    }
  }
}
