package uk.gov.hmrc.agentmappingfrontend.support

import uk.gov.hmrc.auth.core.InsufficientEnrolments

case class SampleUser(
  authoriseJsonResponse: String,
  activeEnrolments: Set[String],
  throwException: Option[Exception] = None)

object SampleUsers {

  val eligibleAgent = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"IR-SA-AGENT", "identifiers": [
       |      { "key":"IRAgentReference", "value": "HZ1234" }
       |    ]}
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    activeEnrolments = Set("IR-SA-AGENT")
  )

  val vatEnrolledAgent = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"HMCE-VAT-AGNT", "identifiers": [
       |      { "key":"AgentRefNo", "value": "HZ1234" }
       |    ]}
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    activeEnrolments = Set("HMCE-VAT-AGNT")
  )

  val saEnrolledAgentInactive = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"IR-SA-AGENT",
       |     "identifiers": [
       |        { "key":"IRAgentReference", "value": "HZ1234" }
       |      ],
       |     "state": "Inactive" }
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    activeEnrolments = Set()
  )

  val vatEnrolledAgentInactive = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"HMCE-VAT-AGNT",
       |     "identifiers": [
       |        { "key":"AgentRefNo", "value": "HZ1234" }
       |      ],
       |     "state": "Inactive" }
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    activeEnrolments = Set()
  )

  val agentNotEnrolled = SampleUser(
    s"""{
       | "allEnrolments": [],
       | "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    activeEnrolments = Set()
  )

  val mtdAgent = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"HMRC-AS-AGENT", "identifiers": [
       |      { "key":"AgentReferenceNumber", "value": "TARN0000001" }
       |    ]}
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    activeEnrolments = Set("HMRC-AS-AGENT")
  )

  val notEligibleAgent = SampleUser(
    s"""{
       |  "allEnrolments": [
       |   { "key":"FOO-AGENT", "identifiers": [
       |      { "key":"fooIdentifier", "value": "foo123" }
       |    ]}
       |  ],
       |  "affinityGroup": "Agent",
       |  "credentials": {
       |    "providerId": "12345-credId",
       |    "providerType": "GovernmentGateway"
       |  }
       |}""".stripMargin,
    activeEnrolments = Set("FOO-AGENT")
  )

  val individual = SampleUser("", Set.empty, Some(InsufficientEnrolments()))
}
