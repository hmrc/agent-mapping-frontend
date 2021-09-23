/*
 * Copyright 2021 HM Revenue & Customs
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

package views

import org.jsoup.Jsoup
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.data.Forms.{nonEmptyText, single}
import play.api.data.{Field, Form, FormError}
import uk.gov.hmrc.agentmappingfrontend.views.html.x_custom_input
import play.api.test.Helpers._
import uk.gov.hmrc.agentmappingfrontend.support.UnitSpec

class XCustomInputSpec extends UnitSpec with GuiceOneAppPerSuite {

  val testForm = Form(
    single(
      "email" -> nonEmptyText
    )
  )

  val messages = stubMessages()

  "x_custom_input" should {

    "render input field with a label" in {

      val view = app.injector.instanceOf[x_custom_input]

      val field =
        Field(testForm, "email", Seq.empty, None, Seq(FormError("email", "email_error")), Some("myemail@example.com"))
      val result = view.render(
        field,
        Array('_label -> "EmailLabel", '_inputHint -> "inputHint", '_hintId -> "hintId", '_inputClass -> "inputClass"),
        messages)

      val doc = Jsoup.parse(contentAsString(result))

      contentAsString(result) should {
        include("Email") and
          include("EmailLabel") and
          include("inputHint") and
          include("email_error")
      }

      doc.getElementsByClass("error-notification").first().text() shouldBe "error.prefix email_error"
      doc.getElementById("hintId").text() shouldBe "inputHint"

    }
  }
}
