package uk.gov.ons.br.vat.repository.hbase

import uk.gov.ons.br.repository.hbase.RowKey
import uk.gov.ons.br.vat.models.VatRef

object VatHBaseRowKey extends (VatRef => RowKey) {
  def apply(vatRef: VatRef): RowKey =
    vatRef.value
}
