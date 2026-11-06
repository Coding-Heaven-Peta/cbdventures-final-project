package za.ac.richfield.cbdventures.domain.enums;

/**
 * What a visitor did. Stored in {@code user_activity.activity_type}.
 *
 * <p>Kept deliberately small: the pilot only needs enough signal to personalise
 * suggestions and to show "recently viewed".</p>
 */
public enum ActivityType {

    /** The visitor opened a business profile. */
    VIEW_PROFILE,

    /** The visitor searched or filtered the catalogue (carries the query terms). */
    SEARCH,

    /** The visitor saved a listing to their shortlist. */
    SAVE_BUSINESS
}
