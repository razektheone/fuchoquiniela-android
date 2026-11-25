package razek.fuchoquiniela.data

import android.content.Context
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import razek.fuchoquiniela.R
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/** The app's language: "es" or "en", whichever Android picked for the app's resources. */
object AppLocale {
    @Volatile
    private var current: Locale = Locale.getDefault()

    fun update(context: Context) {
        current = ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()
    }

    fun languageCode(): String = if (current.language == "en") "en" else "es"

    /** The app's language with the user's region, so dates read naturally ("sáb 22 nov" / "Sat, Nov 22"). */
    fun locale(): Locale {
        val region = Resources.getSystem().configuration.locales[0]?.country?.takeIf { it.isNotEmpty() }
            ?: if (languageCode() == "es") "MX" else "US"
        return Locale(languageCode(), region)
    }
}

/** Date formats shared by the screens, in the device's time zone and the app's language. */
object DateText {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    private fun pattern(skeleton: String): DateTimeFormatter {
        val locale = AppLocale.locale()
        val best = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
        return DateTimeFormatter.ofPattern(best, locale)
    }

    private fun relative(context: Context, instant: Instant): String? {
        val day = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when (day) {
            today -> context.getString(R.string.today)
            today.plusDays(1) -> context.getString(R.string.tomorrow)
            today.minusDays(1) -> context.getString(R.string.yesterday)
            else -> null
        }
    }

    /** "Hoy", "Mañana" or "sáb 22 nov". */
    fun day(context: Context, instant: Instant): String =
        relative(context, instant) ?: pattern("EEEdMMM").format(instant.atZone(zone)).replace(".", "")

    /** "sábado, 22 de noviembre" for section headers. */
    fun longDay(context: Context, instant: Instant): String =
        relative(context, instant) ?: pattern("EEEEdMMMM").format(instant.atZone(zone))

    fun time(instant: Instant): String {
        val locale = AppLocale.locale()
        return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(instant.atZone(zone))
    }

    /** "HOY · 19:05", "SÁB 22 NOV · 19:05". */
    fun kickoff(context: Context, instant: Instant): String =
        "${day(context, instant)} · ${time(instant)}".uppercase(AppLocale.locale())

    /** "22 de noviembre de 2026". */
    fun long(instant: Instant): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(AppLocale.locale()).format(instant.atZone(zone))

    /** "26/11" (or "11/26" in English), for the Liguilla legs. */
    fun dayMonth(instant: Instant): String = pattern("ddMM").format(instant.atZone(zone))

    /** "Cierra en 3 h" when kickoff (when picks lock) is less than a day away. */
    fun closesIn(context: Context, kickoff: Instant, now: Instant = Instant.now()): String? {
        val seconds = kickoff.epochSecond - now.epochSecond
        if (seconds <= 0 || seconds >= 24 * 3600) return null
        val minutes = (seconds / 60).toInt()
        return if (minutes < 60) {
            context.getString(R.string.closes_in_min, maxOf(minutes, 1))
        } else {
            context.getString(R.string.closes_in_h, minutes / 60)
        }
    }
}

/** Pools are in Mexican pesos on the website ("$100"). */
fun money(value: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
    format.currency = Currency.getInstance("MXN")
    format.maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
    return format.format(value)
}

/** Reads a pool uuid from the web invite link (…/pool/{uuid}/join) or the bare code. */
fun poolUuid(text: String): String? =
    Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}").find(text.trim())?.value
