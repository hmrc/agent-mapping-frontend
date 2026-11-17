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

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.bson.BsonDocument
import play.api.libs.ws.WSResponse
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.agentmappingfrontend.model.IRAgentReference
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.stubs.MappingStubs.*
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers.*
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.lock.MongoLockRepository
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

class MappingCheckControllerISpec
extends BaseControllerISpec
with AuthStubs:

  private lazy val lockRepository = app.injector.instanceOf[MongoLockRepository]
  private val enrolmentType = IRAgentReference
  private val returnUrl = "/legacy-service?foo=bar&baz=qux"
  private val continueUrl = "/agent-mapping/continue"
  private val continueUrlWithReturnUrl = s"$continueUrl?returnUrl=${java.net.URLEncoder.encode(returnUrl, "UTF-8")}"
  private val sessionMarker = s"mapping-checked-${enrolmentType.serviceKey}"
  private val mappingStatusPath = s"/agent-mapping/mapping-status/service/${enrolmentType.serviceKey}/value/$saAgentCode"

  override def beforeEach(): Unit =
    super.beforeEach()
    await(lockRepository.collection.deleteMany(BsonDocument()).toFuture())

  private def checkMapping(): WSResponse =
    val url = routes.MappingCheckController.checkMapping(enrolmentType, RedirectUrl(returnUrl)).url
    get(url)()

  "GET /mapping-check/:enrolmentType" should {
    "redirect to the return URL and mark the session when the agent is mapped" in {
      givenUserIsAuthenticated(eligibleAgent)
      givenMappingStatus(
        saAgentCode,
        enrolmentType,
        NO_CONTENT
      )

      val result = checkMapping()

      result.status shouldBe SEE_OTHER
      result.header("Location") shouldBe Some(returnUrl)
      extractSessionCookie(result).value.get(sessionMarker) shouldBe Some("true")
    }

    "redirect to the mandated mapping journey with the return URL" in {
      givenUserIsAuthenticated(eligibleAgent)
      givenMappingStatus(
        saAgentCode,
        enrolmentType,
        NOT_FOUND,
        Some(continueUrl)
      )

      val result = checkMapping()

      result.status shouldBe SEE_OTHER
      result.header("Location") shouldBe Some(continueUrlWithReturnUrl)
      extractSessionCookie(result).value.get(sessionMarker) shouldBe None
    }

    "redirect to the optional mapping journey with the return URL when the agent has not been notified" in {
      givenUserIsAuthenticated(eligibleAgent)
      givenMappingStatus(
        saAgentCode,
        enrolmentType,
        OK,
        Some(continueUrl)
      )

      val result = checkMapping()

      result.status shouldBe SEE_OTHER
      result.header("Location") shouldBe Some(continueUrlWithReturnUrl)
      extractSessionCookie(result).value.get(sessionMarker) shouldBe None
    }

    "redirect to the return URL and mark the session when the agent has already been notified" in {
      givenUserIsAuthenticated(eligibleAgent)
      givenMappingStatus(
        saAgentCode,
        enrolmentType,
        OK,
        Some(continueUrl)
      )

      val firstResult = checkMapping()
      firstResult.status shouldBe SEE_OTHER
      firstResult.header("Location") shouldBe Some(continueUrlWithReturnUrl)
      extractSessionCookie(firstResult).value.get(sessionMarker) shouldBe None

      val result = checkMapping()

      result.status shouldBe SEE_OTHER
      result.header("Location") shouldBe Some(returnUrl)
      extractSessionCookie(result).value.get(sessionMarker) shouldBe Some("true")
    }

    "redirect to the suspension journey without a return URL or session marker when the agent is suspended" in {
      givenUserIsAuthenticated(eligibleAgent)
      givenMappingStatus(
        saAgentCode,
        enrolmentType,
        CONFLICT,
        Some(continueUrl)
      )

      val result = checkMapping()

      result.status shouldBe SEE_OTHER
      result.header("Location") shouldBe Some(continueUrl)
      extractSessionCookie(result).value.get(sessionMarker) shouldBe None
    }

    "redirect to the return URL without checking mapping status when the required enrolment is missing" in {
      givenUserIsAuthenticated(agentNotEnrolled)

      val result = checkMapping()

      result.status shouldBe SEE_OTHER
      result.header("Location") shouldBe Some(returnUrl)
      extractSessionCookie(result).value.get(sessionMarker) shouldBe None
      verify(0, getRequestedFor(urlEqualTo(mappingStatusPath)))
    }

    "redirect to the return URL without checking mapping status when the required enrolment is inactive" in {
      givenUserIsAuthenticated(saEnrolledAgentInactive)

      val result = checkMapping()

      result.status shouldBe SEE_OTHER
      result.header("Location") shouldBe Some(returnUrl)
      extractSessionCookie(result).value.get(sessionMarker) shouldBe None
      verify(0, getRequestedFor(urlEqualTo(mappingStatusPath)))
    }
  }
