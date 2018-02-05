
package fixtures

import models.{Metadata, MetadataResponse}
import play.api.libs.json.{JsValue, Json}

trait MetadataFixtures {

  val testInternalId: String
  val testRegistrationId: String

  def buildMetadataJson(internalId: String = testInternalId): JsValue = Json.parse(
    s"""
       |{
       | "internalId":"$internalId",
       | "registrationID":"regId",
       | "formCreationTimestamp":"2001-12-31T12:00:00Z",
       | "language":"ENG",
       | "submissionResponseEmail":"email@test.com",
       | "completionCapacity":"Director",
       | "declareAccurateAndComplete":true
       |}""".stripMargin)

  def buildMetadata(internalId: String = testInternalId, regId: String = testRegistrationId): Metadata = Metadata(
    internalId = internalId,
    registrationID = regId,
    formCreationTimestamp = "2001-12-31T12:00:00Z",
    language = "ENG",
    submissionResponseEmail = Some("email@test.com"),
    completionCapacity = Some("Director"),
    declareAccurateAndComplete = true
  )

  def buildMetadataResponse(regId: String = testRegistrationId) = {
    MetadataResponse(
      registrationID = regId,
      formCreationTimestamp = "2001-12-31T12:00:00Z",
      language = "ENG",
      completionCapacity = Some("Director")
    )
  }

  def buildMetadataResponseJson(regId: String = testRegistrationId, lang: String = "ENG", cc: String = "Director") =
    Json.parse(s"""{
                  |"registrationID": "$regId",
                  |"formCreationTimestamp": "2001-12-31T12:00:00Z",
                  |"language": "$lang",
                  |"completionCapacity": "$cc"
                  |}""".stripMargin)

  val invalidUpdateJson =
    Json.parse(s"""{
                  |"completionCapacity": "Fake"
                  |}""".stripMargin)
}
