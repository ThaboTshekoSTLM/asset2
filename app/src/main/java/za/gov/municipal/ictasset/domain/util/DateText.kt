package za.gov.municipal.ictasset.domain.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateText {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    fun date(millis: Long?): String =
        millis?.let { dateFormat.format(Date(it)) }.orEmpty()

    fun dateTime(millis: Long?): String =
        millis?.let { dateTimeFormat.format(Date(it)) }.orEmpty()
}
