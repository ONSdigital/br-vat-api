package uk.gov.ons.br.vat


import com.github.tomakehurst.wiremock.client.MappingBuilder
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.{BAD_REQUEST, CONFLICT, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT, UNPROCESSABLE_ENTITY, UNSUPPORTED_MEDIA_TYPE}
import play.mvc.Http.MimeTypes.JSON
import uk.gov.ons.br.vat.EditVatAcceptanceSpec.{ClericallyEditedBy, EditVatRef, HBaseCheckAndUpdateUbrnRequestBody, HBaseVatQueryResponseBody, IncorrectUBRN, InvalidJson, InvalidPatch, JsonPatchContentType, NoMatchFoundResponse, TargetUBRN, UserId, aVatQuery, anUpdateVatRequest}
import uk.gov.ons.br.vat.models.VatRef
import uk.gov.ons.br.vat.test.matchers.HBaseRequestWithFuzzyEditHistoryBodyMatcher.{FuzzyValue, aRequestBodyWithFuzzyEditHistoryLike}
import uk.gov.ons.br.test.hbase.{AbstractServerAcceptanceSpec, HBaseJsonBodyBuilder, HBaseJsonRequestBuilder}
import uk.gov.ons.br.test.matchers.HttpServerErrorStatusCodeMatcher.aServerError

class EditVatAcceptanceSpec extends AbstractServerAcceptanceSpec {

  // must match that configured in src/it/resources/it_application.conf
  override val HBasePort: Int = 8075

  info("As a data editor")
  info("I want to clerically update the linkage between VAT and Legal Units")
  info("So that I can maintain the quality of the register")

  feature("maintain the Unique Business Reference Number (UBRN) of a VAT admin unit") {
    scenario("when a successful update") { wsClient =>
      Given(s"VAT admin data exists with VAT reference ${EditVatRef.value}")
      stubHBaseFor(aVatQuery(withVatRef = EditVatRef).willReturn(
        anOkResponse().withBody(HBaseVatQueryResponseBody)))
      And(s"a Legal Unit exists that is identified by UBRN $TargetUBRN")
      And(s"a database request to update the UBRN from $IncorrectUBRN to $TargetUBRN will succeed")
      stubHBaseFor(anUpdateVatRequest(withVatRef = EditVatRef).
        andMatching(aRequestBodyWithFuzzyEditHistoryLike(HBaseCheckAndUpdateUbrnRequestBody)).
        willReturn(anOkResponse()))

      When(s"a clerical edit of the VAT unit with reference ${EditVatRef.value} requests the UBRN is updated from $IncorrectUBRN to $TargetUBRN")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("a Success response is returned")
      response.status shouldBe NO_CONTENT
    }

    scenario("when another user has concurrently modified the VAT admin unit") { wsClient =>
      Given(s"VAT admin data exists with VAT reference ${EditVatRef.value}")
      stubHBaseFor(aVatQuery(withVatRef = EditVatRef).willReturn(
        anOkResponse().withBody(HBaseVatQueryResponseBody)))
      And(s"a Legal Unit exists that is identified by UBRN $TargetUBRN")
      And(s"a database request to update the UBRN from $IncorrectUBRN to $TargetUBRN will not succeed because of another user's change")
      stubHBaseFor(anUpdateVatRequest(withVatRef = EditVatRef).
        andMatching(aRequestBodyWithFuzzyEditHistoryLike(HBaseCheckAndUpdateUbrnRequestBody)).
        willReturn(aNotModifiedResponse()))

      When(s"a clerical edit of the VAT unit with reference ${EditVatRef.value} requests the UBRN is updated from $IncorrectUBRN to $TargetUBRN")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("a Conflict response is returned")
      response.status shouldBe CONFLICT
    }
  }

  feature("validate the target VAT reference") {
    scenario("when the target VAT reference does not adhere to the expected format") { wsClient =>
      Given("that a valid VAT reference comprises 12 digits")

      When("a clerical edit of a VAT unit with a VAT reference containing alphabetic characters is requested")
      val response = await(wsClient.url(s"/v1/vat/123456X89012").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("a Bad Request response is returned")
      response.status shouldBe BAD_REQUEST
    }

    scenario("when the target VAT admin unit does not exist in the register") { wsClient =>
      Given(s"VAT admin data does not exist with VAT reference ${EditVatRef.value}")
      stubHBaseFor(aVatQuery(withVatRef = EditVatRef).willReturn(
        anOkResponse().withBody(NoMatchFoundResponse)))

      When(s"a clerical edit of the VAT unit with reference ${EditVatRef.value} requests the UBRN is updated from $IncorrectUBRN to $TargetUBRN")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("a Not Found response is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("validate the clerical edit specification (the request body)") {
    scenario("when the clerical edit specification does not have the Json Patch media type") { wsClient =>
      Given(s"that the media type of a Json Patch is $JsonPatchContentType")

      When(s"a clerical edit of a VAT admin unit is requested with a content type of $JSON")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JSON, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("an Unsupported Media Type response is returned")
      response.status shouldBe UNSUPPORTED_MEDIA_TYPE
    }

    scenario("when the clerical edit specification is not a valid Json document") { wsClient =>
      When("a clerical edit of a VAT admin unit is requested with a body that is not valid Json ")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType).
        patch(InvalidJson))

      Then("a Bad Request response is returned")
      response.status shouldBe BAD_REQUEST
    }

    scenario("when the clerical edit specification does not comply with the Json Patch Specification") { wsClient =>
      Given("that the Json Patch Specification does not define an operation named 'update'")

      When("a clerical edit of a VAT admin unit is requested with a Json body that requests an 'update' operation")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(InvalidPatch))

      Then("a Bad Request response is returned")
      response.status shouldBe BAD_REQUEST
    }

    scenario("when the clerical edit specification complies with the Json Patch Specification but requests an unsupported edit") { wsClient =>
      When(s"a clerical edit of the VAT unit with reference ${EditVatRef.value} requests that the name is updated")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/name", "value": "Big Box Co"},
                  | {"op": "replace", "path": "/name", "value": "Big Box Company"}]""".stripMargin))

      Then("a Unprocessable Entity response is returned")
      response.status shouldBe UNPROCESSABLE_ENTITY
    }

    scenario("when the clerical edit specification requests the parent UBRN is changed to a value that does not comply with the UBRN format") { wsClient =>
      Given("that a valid UBRN consists of 16 digits")

      When(s"a clerical edit of the VAT unit with reference ${EditVatRef.value} requests the UBRN is updated to a value that is not 16 digits")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "ABC123"}]""".stripMargin))

      Then("a Unprocessable Entity response is returned")
      response.status shouldBe UNPROCESSABLE_ENTITY
    }
  }

  feature("validate the edit meta data (supplied via request headers)") {
    scenario("when the user id is unspecified") { wsClient =>
      When(s"a clerical edit of a VAT unit is requested without specifying the editing user id")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("a Bad Request response is returned")
      response.status shouldBe BAD_REQUEST
    }
  }

  feature("handle failure gracefully") {
    scenario("when the database server is unavailable") { wsClient =>
      Given("the database server is unavailable")
      stopMockHBase()

      When(s"a clerical edit of the VAT unit with reference ${EditVatRef.value} requests the UBRN is updated from $IncorrectUBRN to $TargetUBRN")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("a server error is returned")
      response.status shouldBe aServerError
    }

    scenario("when the database server will return an error response to an edit request") { wsClient =>
      Given(s"VAT admin data exists with VAT reference ${EditVatRef.value}")
      stubHBaseFor(aVatQuery(withVatRef = EditVatRef).willReturn(
        anOkResponse().withBody(HBaseVatQueryResponseBody)))
      And(s"a Legal Unit exists that is identified by UBRN $TargetUBRN")
      And("the database server will return an error response when a VAT edit is requested")
      stubHBaseFor(anUpdateVatRequest(withVatRef = EditVatRef).
        andMatching(aRequestBodyWithFuzzyEditHistoryLike(HBaseCheckAndUpdateUbrnRequestBody)).
        willReturn(aServiceUnavailableResponse()))

      When(s"a clerical edit of the VAT unit with reference ${EditVatRef.value} requests the UBRN is updated from $IncorrectUBRN to $TargetUBRN")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("a an Internal Server Error is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }

    scenario("when the database server will return an error response to a read request") { wsClient =>
      Given("the database server will return an error response when VAT data is requested")
      stubHBaseFor(aVatQuery(withVatRef = EditVatRef).willReturn(aServiceUnavailableResponse()))
      And(s"a Legal Unit exists that is identified by UBRN $TargetUBRN")
      And(s"a database request to update the UBRN from $IncorrectUBRN to $TargetUBRN will succeed")
      stubHBaseFor(anUpdateVatRequest(withVatRef = EditVatRef).
        andMatching(aRequestBodyWithFuzzyEditHistoryLike(HBaseCheckAndUpdateUbrnRequestBody)).
        willReturn(anOkResponse()))

      When(s"a clerical edit of the VAT unit with reference ${EditVatRef.value} requests the UBRN is updated from $IncorrectUBRN to $TargetUBRN")
      val response = await(wsClient.url(s"/v1/vat/${EditVatRef.value}").
        withHttpHeaders(CONTENT_TYPE -> JsonPatchContentType, ClericallyEditedBy -> UserId).
        patch(s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"},
                  | {"op": "replace", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin))

      Then("a an Internal Server Error is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}

private object EditVatAcceptanceSpec extends HBaseJsonRequestBuilder with HBaseJsonBodyBuilder {
  private val EditVatRef = VatRef("142536478376")
  private val IncorrectUBRN = "1000012345000000"
  private val TargetUBRN = "1000012345000999"
  private val JsonPatchContentType = "application/json-patch+json"
  private val InvalidJson = "[}"
  private val InvalidPatch =
    s"""|[{"op": "test", "path": "/links/ubrn", "value": "$IncorrectUBRN"}
        | {"op": "update", "path": "/links/ubrn", "value": "$TargetUBRN"}]""".stripMargin  // no such 'op'
  private val DataColumnFamily = "d"
  private val EditHistoryColumnFamily = "h"
  private val ParentLinkQualifier = "ubrn"
  private val ClericallyEditedBy = "X-User-Id"
  private val UserId = "jdoe"

  private val HBaseCheckAndUpdateUbrnRequestBody =
    aBodyWith(
      aRowWith(key = EditVatRef.value,
        aColumnWith(DataColumnFamily, qualifier = ParentLinkQualifier, value = TargetUBRN, timestamp = None),
        aColumnWith(EditHistoryColumnFamily, qualifier = FuzzyValue, value = s"$UserId~$FuzzyValue~$IncorrectUBRN~$TargetUBRN", timestamp = None),
        aColumnWith(DataColumnFamily, qualifier = ParentLinkQualifier, value = IncorrectUBRN, timestamp = None)
      )
    )

  private val HBaseVatQueryResponseBody =
    aBodyWith(
      aRowWith(key = EditVatRef.value,
        // admin data
        aColumnWith(DataColumnFamily, qualifier = "vatref", value = EditVatRef.value),
        aColumnWith(DataColumnFamily, qualifier = "legalstatus", value = "A"),
        aColumnWith(DataColumnFamily, qualifier = "nameline1", value = "VDEPJ0IVE5"),
        aColumnWith(DataColumnFamily, qualifier = "address1", value = "VFHLNA0MSJ"),
        aColumnWith(DataColumnFamily, qualifier = "postcode", value = "K6ZL 4GL"),
        // link data
        aColumnWith(DataColumnFamily, qualifier = "ubrn", value = IncorrectUBRN)
      )
    )

  // must match the configuration at src/it/resources/it_application.conf
  private val Namespace = "br_vat_db"
  private val TableName = "vat"
  private val auth = Authorization(username = "br_vat_usr", password = "br_vat_pwd")

  private def anUpdateVatRequest(withVatRef: VatRef): MappingBuilder =
    checkedPutHBaseJson(Namespace, TableName, rowKey = withVatRef.value, auth)

  private def aVatQuery(withVatRef: VatRef): MappingBuilder =
    getHBaseJson(Namespace, TableName, rowKey = withVatRef.value, auth)
}