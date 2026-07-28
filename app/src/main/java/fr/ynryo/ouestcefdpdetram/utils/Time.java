package fr.ynryo.ouestcefdpdetram.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Type et utilitaire temporel unifié pour l'ensemble du projet.
 * Enveloppe à la fois un Instant (horodatage absolu) et un LocalTime (heure locale HH:mm).
 *
 * @author Ynryo
 */
public class Time implements Comparable<Time> {
    /**
     * Formateur standard HH:mm.
     */
    public static final DateTimeFormatter FORMATTER_HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final Instant instant;
    private final LocalTime localTime;

    /**
     * Crée une instance de Time à partir d'un Instant.
     *
     * @param instant L'horodatage absolu.
     */
    public Time(@NonNull Instant instant) {
        this.instant = instant;
        this.localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime();
    }

    /**
     * Crée une instance de Time à partir d'un LocalTime.
     *
     * @param localTime L'heure locale (ex: 14:30).
     */
    public Time(@NonNull LocalTime localTime) {
        this.localTime = localTime;
        this.instant = null;
    }

    // ==================== FACTORY METHODS ====================

    /**
     * Renvoie l'instant présent enveloppé dans un objet Time.
     */
    @NonNull
    public static Time now() {
        return new Time(Instant.now());
    }

    /**
     * Crée un objet Time à partir d'un Instant (ou null si l'argument est null).
     */
    @Nullable
    public static Time from(@Nullable Instant instant) {
        return instant != null ? new Time(instant) : null;
    }

    /**
     * Crée un objet Time à partir d'un LocalTime (ou null si l'argument est null).
     */
    @Nullable
    public static Time from(@Nullable LocalTime localTime) {
        return localTime != null ? new Time(localTime) : null;
    }

    /**
     * Analysateur sécurisé de chaîne de caractères temporelle (ISO-8601, HH:mm:ss, HH:mm).
     *
     * @param timeString Chaîne d'horodatage à analyser.
     * @return L'objet Time correspondant, ou null en cas d'erreur/null.
     */
    @Nullable
    public static Time parse(@Nullable String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }

        try {
            if (timeString.contains("T")) {
                try {
                    ZonedDateTime zdt = ZonedDateTime.parse(timeString);
                    return new Time(zdt.toInstant());
                } catch (Exception e) {
                    LocalDateTime ldt = LocalDateTime.parse(timeString);
                    return new Time(ldt.atZone(ZoneId.systemDefault()).toInstant());
                }
            }

            if (timeString.matches("\\d{2}:\\d{2}:\\d{2}")) {
                LocalTime lt = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss"));
                return new Time(lt);
            }

            if (timeString.matches("\\d{2}:\\d{2}")) {
                LocalTime lt = LocalTime.parse(timeString, FORMATTER_HH_MM);
                return new Time(lt);
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    // ==================== CALCULS & UTILITAIRES ====================

    /**
     * Calcule le retard en minutes entre l'horaire visé et l'horaire estimé/réel.
     *
     * @param aimedTime    Horaire théorique/prévu.
     * @param expectedTime Horaire estimé/réel.
     * @return Retard en minutes, ou null si une donnée est invalide.
     */
    @Nullable
    public static Long calculateDelayMinutes(@Nullable Time aimedTime, @Nullable Time expectedTime) {
        return minutesBetween(aimedTime, expectedTime);
    }

    /**
     * Calcule le retard en minutes entre les chaînes d'horaires théoriques et estimées.
     *
     * @param aimedTimeStr    Horaire théorique/prévu.
     * @param expectedTimeStr Horaire estimé/réel.
     * @return Retard en minutes, ou null si une donnée est invalide.
     */
    @Nullable
    public static Long calculateDelayMinutes(@Nullable String aimedTimeStr, @Nullable String expectedTimeStr) {
        return calculateDelayMinutes(Time.parse(aimedTimeStr), Time.parse(expectedTimeStr));
    }

    /**
     * Calcule la durée en minutes entre deux objets Time.
     *
     * @param start Heure de début.
     * @param end   Heure de fin.
     * @return Durée en minutes, ou null si l'une des heures est nulle.
     */
    @Nullable
    public static Long minutesBetween(@Nullable Time start, @Nullable Time end) {
        if (start == null || end == null || start.getLocalTime() == null || end.getLocalTime() == null) {
            return null;
        }
        return ChronoUnit.MINUTES.between(start.getLocalTime(), end.getLocalTime());
    }

    // ==================== FORMATTAGE ====================

    /**
     * Formate cet objet Time selon un formateur personnalisé.
     */
    @NonNull
    public String format(@NonNull DateTimeFormatter formatter) {
        if (localTime != null) {
            return localTime.format(formatter);
        }
        if (instant != null) {
            return formatter.format(instant.atZone(ZoneId.systemDefault()));
        }
        return "—";
    }

    /**
     * Formate cet objet Time au format "HH:mm".
     */
    @NonNull
    public String formatHHmm() {
        if (localTime == null) return "—";
        return localTime.format(FORMATTER_HH_MM);
    }

    /**
     * Formate un objet Time au format "HH:mm" de manière sécurisée (renvoie "—" si null).
     */
    @NonNull
    public static String formatHHmm(@Nullable Time time) {
        return time != null ? time.formatHHmm() : "—";
    }

    /**
     * Formate une durée en minutes sous la forme "Xm" (ex: "5m").
     */
    @NonNull
    public static String formatDuration(@Nullable Long minutes) {
        if (minutes == null || minutes < 0) {
            return "—";
        }
        if (minutes == 0) {
            return "0m";
        }
        return minutes + "m";
    }

    // ==================== GETTERS ====================

    /**
     * Renvoie l'Instant sous-jacent (ou null si créé uniquement depuis un LocalTime).
     */
    @Nullable
    public Instant getInstant() {
        return instant;
    }

    /**
     * Renvoie le LocalTime sous-jacent.
     */
    @Nullable
    public LocalTime getLocalTime() {
        return localTime;
    }

    // ==================== EQUALS & COMPARE ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Time time = (Time) o;
        return Objects.equals(instant, time.instant) && Objects.equals(localTime, time.localTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(instant, localTime);
    }

    @Override
    public int compareTo(@NonNull Time other) {
        if (this.instant != null && other.instant != null) {
            return this.instant.compareTo(other.instant);
        }
        if (this.localTime != null && other.localTime != null) {
            return this.localTime.compareTo(other.localTime);
        }
        return 0;
    }

    @NonNull
    @Override
    public String toString() {
        return formatHHmm();
    }
}
