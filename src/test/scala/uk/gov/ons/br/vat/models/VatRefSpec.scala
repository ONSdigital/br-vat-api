package uk.gov.ons.br.vat.models

import play.api.libs.json.{JsString, Json}
import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.vat.test.SampleVat.SampleVatRef

class VatRefSpec extends UnitSpec {

  private trait Fixture {
    val VatRefModel = SampleVatRef
    val VatRefUnderlyingValue = VatRefModel.value
  }

  "A VatRef" - {
    "is written to Json as a simple string value" in new Fixture {
      Json.toJson(VatRefModel) shouldBe JsString(VatRefUnderlyingValue)
    }

    "can be bound from a URL path parameter" in new Fixture {
      VatRef.pathBindable.bind("some-key", VatRefUnderlyingValue).right.value shouldBe VatRefModel
    }

    "can be unbound to a URL path parameter" in new Fixture {
      VatRef.pathBindable.unbind("some-key", VatRefModel) shouldBe VatRefUnderlyingValue
    }
  }
}
