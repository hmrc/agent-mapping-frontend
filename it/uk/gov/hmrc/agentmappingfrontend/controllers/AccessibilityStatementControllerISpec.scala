package uk.gov.hmrc.agentmappingfrontend.controllers

import org.scalatest.matchers.{MatchResult, Matcher}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames
import play.api.test.Helpers._

class AccessibilityStatementControllerISpec extends BaseControllerISpec {

  private lazy val controller = app.injector.instanceOf[AccessibilityStatementController]

  "GET /accessibility-statement" should {
    "show the accessibility statement content and accessibility link" in {
      val result = await(controller.showAccessibilityStatement(FakeRequest().withHeaders(HeaderNames.REFERER -> "foo")))

      status(result) shouldBe 200
      result should containMessages("accessibility.statement.h1")
      result should containSubstrings("/contact/accessibility?service=AOSS&userAction=foo")
     }
  }

  protected def containMessages(expectedMessageKeys: String*): Matcher[Result] = {
    new Matcher[Result] {
      override def apply(result: Result): MatchResult = {
        expectedMessageKeys.foreach(checkMessageIsDefined)
        checkIsHtml200(result)

        val resultBody = bodyOf(result)
        val (msgsPresent, msgsMissing) = expectedMessageKeys.partition { messageKey =>
          resultBody.contains(htmlEscapedMessage(messageKey))
        }

        MatchResult(
          msgsMissing.isEmpty,
          s"Content is missing in the response for message keys: ${msgsMissing.mkString(", ")}",
          s"Content is present in the response for message keys: ${msgsPresent.mkString(", ")}"
        )
      }
    }
  }
}