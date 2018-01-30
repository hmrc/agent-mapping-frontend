package uk.gov.hmrc.agentmappingfrontend.model

import uk.gov.hmrc.play.test.UnitSpec

class IdentifierSpec extends UnitSpec {

  "Identifier" should {
    "parse a string having valid format" in {
      Identifier.parse("FOo~BaR") shouldBe Identifier("FOo", "BaR")
    }
    "throw an exception if string representation is not valid" in {
      an[IllegalArgumentException] shouldBe thrownBy(Identifier.parse(""))
      an[IllegalArgumentException] shouldBe thrownBy(Identifier.parse("FOoBar"))
      an[IllegalArgumentException] shouldBe thrownBy(Identifier.parse("FOo~Bar~123"))
    }
  }

}
