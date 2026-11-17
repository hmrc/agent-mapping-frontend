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

package uk.gov.hmrc.agentmappingfrontend.support

import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Writes
import play.api.libs.ws.DefaultBodyWritables.writeableOf_urlEncodedForm
import play.api.libs.ws.DefaultWSCookie
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSCookie
import play.api.libs.ws.WSRequest
import play.api.libs.ws.WSResponse
import play.api.libs.ws.writeableOf_String
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.mvc.Request
import play.api.mvc.Session
import play.api.mvc.SessionCookieBaker
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import play.api.test.Helpers.defaultAwaitTimeout
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCrypto

trait WSHelper:
  this: GuiceOneServerPerSuite =>

  implicit lazy val ws: WSClient = app.injector.instanceOf[WSClient]

  def get[T](uri: String)(queryParam: Seq[(String, String)] = Nil): WSResponse =
    await(
      buildClient(uri)
        .withHttpHeaders("Authorization" -> "Bearer 123")
        .withQueryStringParameters(queryParam*)
        .get()
    )

  // Form submission POST with form body
  def postForm(uri: String)(
    body: Map[String, Seq[String]],
    queryParam: Seq[(String, String)] = Nil
  ): WSResponse =
    await(
      buildClient(uri)
        .withHttpHeaders("Authorization" -> "Bearer 123", "Csrf-Token" -> "nocheck")
        .withQueryStringParameters(queryParam*)
        .post(body)
    )

  // API POST with JSON body
  def post[T](uri: String)(
    body: T,
    queryParam: Seq[(String, String)] = Nil
  )(implicit writes: Writes[T]): WSResponse =
    await(
      buildClient(uri)
        .withHttpHeaders("Authorization" -> "Bearer 123", "Csrf-Token" -> "nocheck")
        .withQueryStringParameters(queryParam*)
        .post(writes.writes(body).toString())
    )

  def put[T](uri: String)(
    body: T,
    queryParam: Seq[(String, String)] = Nil
  )(implicit writes: Writes[T]): WSResponse =
    await(
      buildClient(uri)
        .withHttpHeaders("Authorization" -> "Bearer 123", "Csrf-Token" -> "nocheck")
        .withQueryStringParameters(queryParam*)
        .put(writes.writes(body).toString())
    )

  def delete[T](uri: String): WSResponse =
    await(
      buildClient(uri)
        .withHttpHeaders("Authorization" -> "Bearer 123")
        .delete()
    )

  val baseUrl: String = "/agent-mapping"

  private def buildClient(path: String): WSRequest =
    ws.url(s"http://localhost:$port$baseUrl${path.replace(baseUrl, "")}")
      .withFollowRedirects(false)
      .withCookies(
        DefaultWSCookie("PLAY_LANG", "en"),
        mockSessionCookie
      )

  val sessionHeaders: Map[String, String] = Map(
    SessionKeys.lastRequestTimestamp -> System.currentTimeMillis().toString,
    SessionKeys.authToken -> "mock-bearer-token",
    SessionKeys.sessionId -> "mock-sessionid"
  )

  implicit val request: Request[AnyContentAsFormUrlEncoded] = FakeRequest().withSession(sessionHeaders.toSeq*).withFormUrlEncodedBody()

  def mockSessionCookie: WSCookie =

    val cookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
    val cookieBaker = app.injector.instanceOf[SessionCookieBaker]
    val sessionCookie = cookieBaker.encodeAsCookie(Session(sessionHeaders))
    val encryptedValue = cookieCrypto.crypto.encrypt(PlainText(sessionCookie.value))
    val cookie = sessionCookie.copy(value = encryptedValue.value)

    new WSCookie():
      override def name: String = cookie.name

      override def value: String = cookie.value

      override def domain: Option[String] = cookie.domain

      override def path: Option[String] = Some(cookie.path)

      override def maxAge: Option[Long] = cookie.maxAge.map(_.toLong)

      override def secure: Boolean = cookie.secure

      override def httpOnly: Boolean = cookie.httpOnly

  def extractSessionCookie(response: WSResponse): Option[Session] =
    response.cookies.find(_.name == "mdtp").map(cookie =>
      val cookieCrypto = app.injector.instanceOf[SessionCookieCrypto]
      val cookieBaker = app.injector.instanceOf[SessionCookieBaker]
      val decryptedValue = cookieCrypto.crypto.decrypt(Crypted(cookie.value))
      Session(cookieBaker.decode(decryptedValue.value))
    )
