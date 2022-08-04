package xmlUnit

import Utils
import org.simpleframework.xml.Text
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Root

@Root(name = "string", strict = true)
data class XMLString(
    @field:Attribute(name = "name")
    var id: String? = null,

    @field:Text(required = false)
    var text: String? = null
) {
    val page by lazy { Utils.getIdHeader(id) }
}