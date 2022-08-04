package xmlUnit

import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

@Root(name = "resources", strict = false)
data class XMLResource(
    @field:ElementList(name = "string", inline = true)
    var entriesList: List<XMLString>? = null
)