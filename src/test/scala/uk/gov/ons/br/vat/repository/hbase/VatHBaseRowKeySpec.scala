package uk.gov.ons.br.vat.repository.hbase

import uk.gov.ons.br.test.UnitSpec
import uk.gov.ons.br.vat.test.SampleVat.SampleVatRef

class VatHBaseRowKeySpec extends UnitSpec {

  "The HBase RowKey for VAT admin units" - {
    "is the VAT reference" in {
      VatHBaseRowKey(SampleVatRef) shouldBe SampleVatRef.value
    }
  }
}
