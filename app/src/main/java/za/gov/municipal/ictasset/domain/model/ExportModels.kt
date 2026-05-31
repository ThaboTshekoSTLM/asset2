package za.gov.municipal.ictasset.domain.model

enum class ExportFormat(val extension: String) {
    PDF("pdf"),
    EXCEL("xls")
}

data class ExportedReport(
    val fileName: String,
    val absolutePath: String
)

data class TabularReport(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>
)
