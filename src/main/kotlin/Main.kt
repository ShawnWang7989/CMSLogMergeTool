import org.simpleframework.xml.core.Persister
import xmlUnit.XMLResource
import xmlUnit.XMLString
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.StringWriter

private enum class LogHeader(val string: String) {
    KEY("key"),
    DEFAULT("default"),
    NEW_STRING("new string"),
    OLD_STRING("old string")
}

private const val DELIMITER = ": "
private const val MERGED_TAIL = ".merged"

fun main(args: Array<String>) {
    val serializer = Persister()
    val stringsList = ArrayList<XMLString>()
    var stringsFile: File? = null
    fileChecker("Please enter the strings file path: ") {
        stringsFile = it
        serializer.read(XMLResource::class.java, it.inputStream())?.entriesList?.apply {
            stringsList.addAll(this)
        }
    }

    if (stringsList.isEmpty()) {
        println("The strings file can't be empty!")
        return
    }

    val logDataList = getLogDataList()

    logDataList.forEach {
        println("key : ${it.id}\ndefault: ${it.defaultString}\nold string: ${it.oldString}\nnew string: ${it.newString}")
        println("------------")
    }
    logDataList.sortedBy { it.id }

    println("\nDo you want to replace these ${logDataList.size} strings?(Y/N)")
    readLine()?.takeIf { it.lowercase() == "y" } ?: return

    val needAddList = logDataList.filter { logData -> stringsList.find { it.id == logData.id } == null }
    val needAddPageMap = HashMap<String, ArrayList<LogData>>()
    needAddList.forEach {
        val list = needAddPageMap[it.page] ?: ArrayList<LogData>().apply { needAddPageMap[it.page] = this }
        list.add(it)
    }
    val needAddMap = HashMap<String, ArrayList<LogData>>()
    var lastPage: String? = null
    stringsList.reversed().forEach {
        if (lastPage != null && it.page != lastPage) {
            needAddPageMap[it.page]?.apply {
                needAddMap[it.id!!] = this
                needAddPageMap.remove(it.page)
            }
        }
        lastPage = it.page
    }
    val newPageList = ArrayList<LogData>()
    needAddPageMap.keys.filter { page -> needAddMap.values.find { list -> list.first().page == page } == null }
        .forEach {
            newPageList.addAll(needAddPageMap[it]!!)
        }
    needAddMap[stringsList.last().id!!]?.apply { addAll(newPageList) }
        ?: run { needAddMap[stringsList.last().id!!] = newPageList }

    val updateMap = HashMap<String, LogData>()
    logDataList.filter { !needAddList.contains(it) }.forEach {
        updateMap[it.id] = it
    }

    try {
        val mergedFile = File("${stringsFile!!.path}$MERGED_TAIL")
        BufferedReader(FileReader(stringsFile!!.path)).use { bufferReader ->
            mergedFile.bufferedWriter().use { bufferWriter ->

                var isFirstTimeWrite = true
                val write: (String?) -> Unit = {
                    if (isFirstTimeWrite) {
                        isFirstTimeWrite = false
                    } else {
                        bufferWriter.newLine()
                    }
                    it?.run { bufferWriter.write(this) }
                }

                var line: String? = null
                while (bufferReader.readLine()?.also { line = it } != null) {

                    var xmlString: XMLString?
                    try {
                        xmlString = serializer.read(XMLString::class.java, line)
                    } catch (e: Exception) {
                        write(line)
                        continue
                    }

                    updateMap[xmlString!!.id]?.apply {
                        val stringWriter = StringWriter()
                        serializer.write(XMLString(id, newString), stringWriter)
                        write("    ${stringWriter.buffer}")
                    } ?: run {
                        write(line)
                    }

                    needAddMap[xmlString.id]?.forEach {
                        val stringWriter = StringWriter()
                        serializer.write(XMLString(it.id, it.newString), stringWriter)
                        write("    ${stringWriter.buffer}")
                    }
                }
            }
        }
        stringsFile!!.delete()
        mergedFile.renameTo(stringsFile!!)
    } catch (e: Exception) {
        println("Write file error!")
    }

    println("Write file success!")
}

private fun skipFirstTimeNewLine() {
}

private fun BufferedReader.readLineSkipBlank(): String? {
    var line: String? = null
    while (readLine()?.also { line = it } != null) {
        if (line!!.isNotBlank()) {
            return line
        }
    }
    return null
}

private fun getLogDataList(): List<LogData> {
    val list = ArrayList<LogData>()
    fileChecker("Please enter the log file path: ") { file ->
        list.clear()
        BufferedReader(FileReader(file)).use { bufferReader ->
            while (true) {
                val key = getHeaderAndValue(LogHeader.KEY, bufferReader.readLineSkipBlank() ?: break) ?: continue
                val default =
                    getHeaderAndValue(LogHeader.DEFAULT, bufferReader.readLineSkipBlank() ?: break) ?: continue
                val oldString =
                    getHeaderAndValue(LogHeader.OLD_STRING, bufferReader.readLineSkipBlank() ?: break) ?: continue
                val newString =
                    getHeaderAndValue(LogHeader.NEW_STRING, bufferReader.readLineSkipBlank() ?: break) ?: continue
                list.add(LogData(key, default, oldString, newString))
            }
        }
    }
    return list
}

private fun getHeaderAndValue(header: LogHeader, string: String): String? {
    val tailIndex = string.indexOf(DELIMITER).takeIf { it >= 0 } ?: return null
    val head = string.substring(0, tailIndex).trim()
    return if (head == header.string) {
        string.substring(tailIndex + DELIMITER.length, string.length)
    } else {
        null
    }
}


private fun fileChecker(requestText: String, fileToDo: (File) -> Unit) {
    while (true) {
        println(requestText)
        val filePath = readLine()
        if (filePath.isNullOrBlank()) {
            println("ERROR! Incorrect file path!")
            continue
        }
        val file = File(filePath)
        if (!file.exists()) {
            println("ERROR! File not existed!")
            continue
        }
        try {
            fileToDo(file)
            break
        } catch (e: Exception) {
            println("ERROR! Read file failed! $e")
        }
    }

}