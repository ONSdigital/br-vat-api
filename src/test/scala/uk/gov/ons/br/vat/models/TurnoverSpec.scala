package uk.gov.ons.br.vat.models

import play.api.libs.json.Json
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.test.json.JsonString
import uk.gov.ons.br.test.json.JsonString.{withInt, withOptionalString}

class TurnoverSpec extends UnitSpec {

  private trait Fixture {
    def expectedJsonStrOf(turnover: Turnover): String =
      JsonString.ofObject(
        withInt(named = "amount", withValue = turnover.amount),
        withOptionalString(named = "date", withValue = turnover.date)
      )
  }

  "Turnover" - {
    "can be represented in Turnover" - {
      "when all fields are defined" in new Fixture {
        val turnover = Turnover(amount = 12345, date = Some("01/02/2018"))

        Json.toJson(turnover) shouldBe Json.parse(expectedJsonStrOf(turnover))
      }

      "when only some fields are defined" in new Fixture {
        val turnover = Turnover(amount = 12345, date = None)

        Json.toJson(turnover) shouldBe Json.parse(expectedJsonStrOf(turnover))
      }
    }
  }
}
