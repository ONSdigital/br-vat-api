package uk.gov.ons.br.vat

import com.github.tomakehurst.wiremock.client.MappingBuilder
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.{Json, Reads}
import play.mvc.Http.MimeTypes.JSON
import uk.gov.ons.br.models.{Address, Lifespan, LinkToLegalUnit}
import uk.gov.ons.br.test.hbase.HBaseJsonBodyBuilder.NoMatchFoundResponse
import uk.gov.ons.br.test.hbase.{AbstractServerAcceptanceSpec, HBaseJsonBodyBuilder, HBaseJsonRequestBuilder}
import uk.gov.ons.br.test.matchers.HttpServerErrorStatusCodeMatcher.aServerError
import uk.gov.ons.br.vat.QueryVatAcceptanceSpec.{TargetVat, TargetVatHBaseResponseBody, TargetVatRef, aVatRequest, readsVat}
import uk.gov.ons.br.vat.models.{Turnover, Vat, VatRef}


class QueryVatAcceptanceSpec extends AbstractServerAcceptanceSpec {

  // must match that configured in src/it/resources/it_application.conf
  override val HBasePort: Int = 8075

  info("As a data explorer")
  info("I want to query VAT admin data")
  info("So that I can build a picture of a business")

  feature("query VAT admin data by VAT reference") {
    scenario("when the target VAT reference matches that of a known unit") { wsClient =>
      Given(s"VAT admin data exists with VAT reference $TargetVatRef")
      stubHBaseFor(aVatRequest(withVatRef = TargetVatRef).willReturn(
        anOkResponse().withBody(TargetVatHBaseResponseBody)
      ))

      When(s"the VAT unit with VAT reference $TargetVatRef is requested")
      val response = await(wsClient.url(s"/v1/vat/${TargetVatRef.value}").get())

      Then(s"the details of the VAT admin data with VAT reference $TargetVatRef are returned")
      response.status shouldBe OK
      response.header(CONTENT_TYPE).value shouldBe JSON
      response.json.as[Vat](readsVat) shouldBe TargetVat
    }

    scenario("when the target VAT reference does not match that of a known unit") { wsClient =>
      Given(s"VAT admin data does not exist with VAT reference $TargetVatRef")
      stubHBaseFor(aVatRequest(withVatRef = TargetVatRef).willReturn(
        anOkResponse().withBody(NoMatchFoundResponse)
      ))

      When(s"the VAT unit with VAT reference $TargetVatRef is requested")
      val response = await(wsClient.url(s"/v1/vat/${TargetVatRef.value}").get())

      Then(s"a Not Found response is returned")
      response.status shouldBe NOT_FOUND
    }
  }

  feature("validate the requested VAT reference") {
    scenario("when the target VAT reference is too long") { wsClient =>
      Given("that a valid VAT reference comprises 12 numeric characters")

      When("the VAT unit with a VAT reference having 13 numeric characters is requested")
      val response = await(wsClient.url(s"/v1/vat/1234567890123").get())

      Then("a Bad Request response is returned")
      response.status shouldBe BAD_REQUEST
    }

    scenario("when the target VAT reference is too short") { wsClient =>
      Given("that a valid VAT reference comprises 12 numeric characters")

      When("the VAT unit with a VAT reference having 11 numeric characters is requested")
      val response = await(wsClient.url(s"/v1/vat/12345678901").get())

      Then("a Bad Request response is returned")
      response.status shouldBe BAD_REQUEST
    }
  }

  feature("handle failure gracefully") {
    scenario("when the database server is unavailable") { wsClient =>
      Given("the database server is unavailable")
      stopMockHBase()

      When(s"the VAT unit with VAT reference $TargetVatRef is requested")
      val response = await(wsClient.url(s"/v1/vat/${TargetVatRef.value}").get())

      Then(s"a server error is returned")
      response.status shouldBe aServerError
    }

    scenario("when the database server returns an error response") { wsClient =>
      Given("the database server will return an error response")
      stubHBaseFor(aVatRequest(withVatRef = TargetVatRef).willReturn(
        aServiceUnavailableResponse()
      ))

      When(s"the VAT unit with VAT reference $TargetVatRef is requested")
      val response = await(wsClient.url(s"/v1/vat/${TargetVatRef.value}").get())

      Then(s"an Internal Server Error is returned")
      response.status shouldBe INTERNAL_SERVER_ERROR
    }
  }
}

object QueryVatAcceptanceSpec extends HBaseJsonRequestBuilder with HBaseJsonBodyBuilder {
  private val ColumnFamily = "d"
  private val TargetVatRef = VatRef("123456789012")
  private val TargetVatHBaseResponseBody =
    aBodyWith(
      aRowWith(key = s"${TargetVatRef.value}",
        // admin data
        aColumnWith(ColumnFamily, qualifier = "entref", value = "5235981614"), // ignored field
        aColumnWith(ColumnFamily, qualifier = "vatref", value = TargetVatRef.value),
        aColumnWith(ColumnFamily, qualifier = "deathcode", value = "658664"),
        aColumnWith(ColumnFamily, qualifier = "deathdate", value = "05/05/2015"),
        aColumnWith(ColumnFamily, qualifier = "birthdate", value = "01/01/2016"),
        aColumnWith(ColumnFamily, qualifier = "turnover", value = "1000"),
        aColumnWith(ColumnFamily, qualifier = "turnoverdate", value = "31/03/2016"),
        aColumnWith(ColumnFamily, qualifier = "legalstatus", value = "A"),
        aColumnWith(ColumnFamily, qualifier = "sic92", value = "10020"),
        aColumnWith(ColumnFamily, qualifier = "recordtype", value = "2"),
        aColumnWith(ColumnFamily, qualifier = "crn", value = "1"), // ignored field
        aColumnWith(ColumnFamily, qualifier = "addressref", value = "9607"), // ignored field
        aColumnWith(ColumnFamily, qualifier = "marker", value = "1"), // ignored field
        aColumnWith(ColumnFamily, qualifier = "inqcode", value = "OR6PHFQ78Q"), // ignored field
        aColumnWith(ColumnFamily, qualifier = "nameline1", value = "VDEPJ0IVE5"),
        aColumnWith(ColumnFamily, qualifier = "nameline2", value = "8JOS45YC8U"),
        aColumnWith(ColumnFamily, qualifier = "nameline3", value = "IEENIUFNHI"),
        aColumnWith(ColumnFamily, qualifier = "tradstyle1", value = "WD45"),
        aColumnWith(ColumnFamily, qualifier = "tradstyle2", value = "L3CS"),
        aColumnWith(ColumnFamily, qualifier = "tradstyle3", value = "U54L"),
        aColumnWith(ColumnFamily, qualifier = "address1", value = "VFHLNA0MSJ"),
        aColumnWith(ColumnFamily, qualifier = "address2", value = "P4FUV3QM7D"),
        aColumnWith(ColumnFamily, qualifier = "address3", value = "5TM1RA3CFR"),
        aColumnWith(ColumnFamily, qualifier = "address4", value = "00N7E1PVVM"),
        aColumnWith(ColumnFamily, qualifier = "address5", value = "HKJY8TOMJ8"),
        aColumnWith(ColumnFamily, qualifier = "postcode", value = "K6ZL 4GL"),
        // link data
        aColumnWith(ColumnFamily, qualifier = "ubrn", value = "1000012345000999")
      )
    )

  // the expected model representation of the above data cells
  private val TargetVat = Vat(
    vatref = TargetVatRef,
    name = "VDEPJ0IVE5" + "8JOS45YC8U" + "IEENIUFNHI",
    address = Address(
      line1 = "VFHLNA0MSJ",
      line2 = Some("P4FUV3QM7D"),
      line3 = Some("5TM1RA3CFR"),
      line4 = Some("00N7E1PVVM"),
      line5 = Some("HKJY8TOMJ8"),
      postcode = "K6ZL 4GL"
    ),
    tradingStyle = Some("WD45" + "L3CS" + "U54L"),
    legalStatus = "A",
    sic = "10020",
    recordType = "2",
    turnover = Some(Turnover(
      amount = Some(1000),
      date = Some("31/03/2016"),
    )),
    lifespan = Some(Lifespan(
      birthDate = "01/01/2016",
      deathDate = Some("05/05/2015"),
      deathCode = Some("658664")
    )),
    links = Some(LinkToLegalUnit("1000012345000999"))
  )

  // must match the configuration at src/it/resources/it_application.conf
  private def aVatRequest(withVatRef: VatRef): MappingBuilder =
    getHBaseJson(namespace = "br_vat_db", tableName = "vat", rowKey = withVatRef.value,
      auth = Authorization(username = "br_vat_usr", password = "br_vat_pwd"))

  private implicit val readsAddress: Reads[Address] = Json.reads[Address]
  private implicit val readsLifespan: Reads[Lifespan] = Json.reads[Lifespan]
  private implicit val readsLinkToLegalUnit: Reads[LinkToLegalUnit] = Json.reads[LinkToLegalUnit]
  private implicit val readsTurnover: Reads[Turnover] = Json.reads[Turnover]
  private implicit val readsVatRef: Reads[VatRef] = StringReads.map(VatRef(_))
  private val readsVat: Reads[Vat] = Json.reads[Vat]
}