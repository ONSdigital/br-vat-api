package uk.gov.ons.br.vat.models

import play.api.libs.json.Writes
import play.api.libs.json.Writes.StringWrites
import play.api.mvc.PathBindable
import play.api.mvc.PathBindable.bindableString

// private constructor dictates that an instance is only available via apply() - allowing us to validate if necessary.
final case class VatRef private(value: String)

object VatRef {
  private def underlying(vatRef: VatRef): String =
    vatRef.value

  // write an instance of the model to JSON as a simple string value
  implicit val writes: Writes[VatRef] =
    Writes((StringWrites.writes _).compose(underlying))

  /*
   * Support binding instances of the model as URL path parameters.
   * Unfortunately this mechanism relies on implicit resolution (an instance bound via guice for example is ignored).
   */
  implicit val pathBindable: PathBindable[VatRef] =
    bindableString.transform(toB = VatRef.apply, toA = underlying)
}
