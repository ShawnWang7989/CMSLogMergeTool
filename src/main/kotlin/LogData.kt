import xmlUnit.XMLString

data class LogData(val id: String, val defaultString:String, val oldString:String, val newString: String) {
    val page by lazy { Utils.getIdHeader(id) }
}