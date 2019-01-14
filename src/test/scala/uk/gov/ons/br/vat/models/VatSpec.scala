package uk.gov.ons.br.vat.models

import play.api.libs.json.Json
import uk.gov.ons.br.models.Lifespan
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.test.json.JsonString
import uk.gov.ons.br.test.json.JsonString._
import uk.gov.ons.br.vat.test.SampleVat._

class VatSpec extends UnitSpec {

  private trait Fixture {
    def expectedJsonStrOf(vat: Vat): String = {
      JsonString.ofObject(
        withString(named = "vatref", withValue = vat.vatref.value),
        withString(named = "name", withValue = vat.name),
        withObject(named = "address",
          withString(named = "line1", withValue = vat.address.line1),
          withOptionalString(named = "line2", withValue = vat.address.line2),
          withOptionalString(named = "line3", withValue = vat.address.line3),
          withOptionalString(named = "line4", withValue = vat.address.line4),
          withOptionalString(named = "line5", withValue = vat.address.line5),
          withString(named = "postcode", withValue = vat.address.postcode)
        ),
        withOptionalString(named = "tradingStyle", withValue = vat.tradingStyle),
        withString(named = "legalStatus", withValue = vat.legalStatus),
        withString(named = "sic", withValue = vat.sic),
        withString(named = "recordType", withValue = vat.recordType),
        withOptionalObject(named = "turnover", vat.turnover.fold(EmptyValues) { turnover =>
          Seq(
            withInt(named = "amount", withValue = turnover.amount),
            withOptionalString(named = "date", withValue = turnover.date)
          )
        }: _*),
        withOptionalObject(named = "lifespan", vat.lifespan.fold(EmptyValues) { lifespan =>
          Seq(
            withString(named = "birthDate", withValue = lifespan.birthDate),
            withOptionalString(named = "deathDate", withValue = lifespan.deathDate),
            withOptionalString(named = "deathCode", withValue = lifespan.deathCode)
          )
        }: _*),
        withOptionalObject(named = "links", vat.links.fold(EmptyValues) { links =>
          Seq(
            withString(named = "ubrn", withValue = links.ubrn)
          )
        }: _*)
      )
    }
  }

  "A VAT admin unit" - {
    "can be represented in Json" - {
      "when all fields are defined" in new Fixture {
        Json.toJson(SampleVatUnitWithAllFields) shouldBe Json.parse(expectedJsonStrOf(SampleVatUnitWithAllFields))
      }

      "when only mandatory fields are defined" in new Fixture {
        Json.toJson(SampleVatUnitWithOnlyMandatoryFields) shouldBe Json.parse(expectedJsonStrOf(SampleVatUnitWithOnlyMandatoryFields))
      }

      "when only some of the lifespan fields are defined" in new Fixture {
        val vatWithPartialLifespan = SampleVatUnitWithAllFields.copy(lifespan = Some(Lifespan(
          birthDate = "06/07/2015", deathDate = Some("08/09/2018"), deathCode = None
        )))

        Json.toJson(vatWithPartialLifespan) shouldBe Json.parse(expectedJsonStrOf(vatWithPartialLifespan))
      }

      "when only some of the turnover fields are defined" in new Fixture {
        val vatWithPartialTurnover = SampleVatUnitWithAllFields.copy(turnover = Some(Turnover(
          amount = 12345, date = None
        )))

        Json.toJson(vatWithPartialTurnover) shouldBe Json.parse(expectedJsonStrOf(vatWithPartialTurnover))
      }
    }
  }
}
