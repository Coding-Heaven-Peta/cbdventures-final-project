package za.ac.richfield.cbdventures.domain;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * BUSINESS_HOURS - opening hours per weekday (Phase 3 normalisation).
 *
 * <p>Adopted from the group's database design: one row per business per day
 * instead of a single flat "Mon-Fri 07:00-17:00" string, so a venue can be open
 * on Saturday and closed on Sunday, and "is it open now?" is answered from the
 * row for today.</p>
 *
 * <p>Integrity rules (also enforced by CHECK constraints in
 * {@code db/schema_mysql.sql}):</p>
 * <ul>
 *   <li>{@code UNIQUE(business_id, day_of_week)} - at most one row per day.</li>
 *   <li>{@code day_of_week} is an ISO weekday number: 1 = Monday ... 7 = Sunday.</li>
 *   <li>When the day is open, both times are required and opening must precede
 *       closing; a closed day stores no times.</li>
 * </ul>
 */
@Entity
@Table(name = "business_hours",
        uniqueConstraints = @UniqueConstraint(name = "uk_business_hours_day",
                columnNames = {"business_id", "day_of_week"}),
        indexes = @Index(name = "idx_business_hours_business", columnList = "business_id"))
public class BusinessHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hours_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private Business business;

    /** ISO weekday: 1 = Monday ... 7 = Sunday, stored as the number. */
    @Convert(converter = DayOfWeekNumber.class)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "is_closed", nullable = false)
    private boolean closed = false;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    public BusinessHours() {
    }

    public BusinessHours(DayOfWeek dayOfWeek, boolean closed, LocalTime openTime, LocalTime closeTime) {
        this.dayOfWeek = dayOfWeek;
        this.closed = closed;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    /** Convenience for seeding: an open day with the same hours every weekday. */
    public static BusinessHours open(DayOfWeek dayOfWeek, LocalTime open, LocalTime close) {
        return new BusinessHours(dayOfWeek, false, open, close);
    }

    /** Convenience for seeding: a closed day. */
    public static BusinessHours closed(DayOfWeek dayOfWeek) {
        return new BusinessHours(dayOfWeek, true, null, null);
    }

    /** True when this row is for the supplied ISO weekday. */
    public boolean isFor(DayOfWeek day) {
        return dayOfWeek != null && dayOfWeek == day;
    }

    /**
     * Whether the venue is inside its trading window at {@code time}.
     * Handles windows that cross midnight (e.g. 18:00 - 02:00).
     */
    public boolean covers(LocalTime time) {
        if (closed || openTime == null || closeTime == null) {
            return false;
        }
        if (closeTime.isAfter(openTime)) {
            return !time.isBefore(openTime) && time.isBefore(closeTime);
        }
        return !time.isBefore(openTime) || time.isBefore(closeTime);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public LocalTime getOpenTime() {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime) {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    /**
     * Stores the ISO weekday as its number (1 = Monday ... 7 = Sunday) so the
     * column stays readable in raw SQL and can be range-checked by the database
     * ({@code CHECK (day_of_week BETWEEN 1 AND 7)}).
     */
    @Converter
    public static class DayOfWeekNumber implements AttributeConverter<DayOfWeek, Integer> {

        @Override
        public Integer convertToDatabaseColumn(DayOfWeek day) {
            return day == null ? null : day.getValue();
        }

        @Override
        public DayOfWeek convertToEntityAttribute(Integer value) {
            return value == null ? null : DayOfWeek.of(value);
        }
    }
}
