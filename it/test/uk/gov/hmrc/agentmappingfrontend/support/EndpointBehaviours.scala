/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentmappingfrontend.support

import org.apache.pekko.stream.Materializer
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.agentmappingfrontend.controllers.routes
import uk.gov.hmrc.agentmappingfrontend.stubs.AuthStubs
import uk.gov.hmrc.agentmappingfrontend.support.SampleUsers._

trait EndpointBehaviours extends AuthStubs {
  me: UnitSpec with WireMockSupport =>
  type PlayRequest = Request[AnyContent] => Result

  protected def fakeRequest(endpointMethod: String, endpointPath: String): FakeRequest[AnyContentAsEmpty.type]
  protected def materializer: Materializer

  implicit lazy val mat = materializer

  protected def anAuthenticatedEndpoint(
    endpointMethod: String,
    endpointPath: String,
    doRequest: Request[AnyContentAsEmpty.type] => Result
  ): Unit =
    "redirect to the sign-in page if the current user is not logged in" in {
      givenUserIsNotAuthenticated()
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header.headers("Location") should include("/bas-gateway/sign-in")
    }

  protected def anEndpointReachableIfSignedInWithEligibleEnrolment(endpointMethod: String, endpointPath: String)(
    doRequest: Request[AnyContentAsEmpty.type] => Result
  ): Unit = {
    behave like anAuthenticatedEndpoint(endpointMethod, endpointPath, doRequest)

    "redirect to /not-enrolled page if the current user has an ineligible enrolment" in {
      givenUserIsAuthenticated(notEligibleAgent)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled(id = "someArnRefForMapping").url
    }

    "redirect to /incorrect-account page if the current user has an HMRC-AS-AGENT enrolment" in {
      givenUserIsAuthenticated(mtdAsAgent)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header
        .headers("Location") shouldBe routes.MappingController.incorrectAccount(id = "someArnRefForMapping").url
    }

    "redirect to /already-linked page if the current user has an HMRC-AGENT-AGENT enrolment" in {
      givenUserIsAuthenticated(mtdAgentAgent)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.alreadyMapped(id = "someArnRefForMapping").url
    }

    "redirect to /not-enrolled page if the current user has no enrolments" in {
      givenUserIsAuthenticated(agentNotEnrolled)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled(id = "someArnRefForMapping").url
    }

    "render the /not-enrolled page if the current user has only inactive enrolments" in {
      givenUserIsAuthenticated(saEnrolledAgentInactive)
      val request = fakeRequest(endpointMethod, endpointPath)
      val result = doRequest(request)

      result.header.status shouldBe 303
      result.header.headers("Location") shouldBe routes.MappingController.notEnrolled(id = "someArnRefForMapping").url
    }
  }

}
