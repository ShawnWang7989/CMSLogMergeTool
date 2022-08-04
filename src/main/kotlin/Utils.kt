object Utils {
    private const val DELIMITER = "__"
    fun getIdHeader(id: String?) = id?.indexOf(DELIMITER)?.takeIf { it >= 0 }?.let { id.substring(0, it) }?:""
}