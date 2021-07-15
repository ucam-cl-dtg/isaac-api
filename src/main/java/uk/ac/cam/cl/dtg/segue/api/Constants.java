/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api;

import com.google.common.collect.ImmutableSet;
import org.postgresql.util.PGInterval;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class to provide common segue-specific constants.
 * 
 */
public final class Constants {
    // General Configuration stuff

    public static final String DEFAULT_TIME_LOCALITY = "Europe/London";

    /**
     * Name to use to describe the application to external services, e.g. 3rd party authenticators.
     */
    public static final String APPLICATION_NAME = "Segue";

    /**
     * Constant representing the key for the HOST_NAME property - Used for establishing the Base URL for fully qualified
     * urls.
     */
    public static final String HOST_NAME = "HOST_NAME";

    // API Email settings

    /**
     * Constant representing the key for the MAILER_SMTP_SERVER property - Address of the SMTP server.
     */
    public static final String MAILER_SMTP_SERVER = "MAILER_SMTP_SERVER";

    /**
     * Constant representing the key for the MAIL_FROM_ADDRESS property - Email address to send mail from.
     */
    public static final String MAIL_FROM_ADDRESS = "MAIL_FROM_ADDRESS";
    
    /**
     * Constant representing reply to address for e-mails.
     */
    public static final String REPLY_TO_ADDRESS = "REPLY_TO_ADDRESS";
    
    /**
     * Constant representing the mail receiver address for contact us e-mails sent by the endpoint.
     */
    public static final String MAIL_RECEIVERS = "MAIL_RECEIVERS";

    /**
     * Constant representing the name emails will be sent from.
     */
    public static final String MAIL_NAME = "MAIL_NAME";
    
    /**
     * Constant representing the key for the SERVER_ADMIN_ADDRESS property - Email address to send admin related emails
     * to.
     */
    public static final String SERVER_ADMIN_ADDRESS = "SERVER_ADMIN_ADDRESS";

    /**
     * Constant representing the text used for {{sig}} in emails..
     */
    public static final String EMAIL_SIGNATURE = "EMAIL_SIGNATURE";

    /**
     * Constant representing the token used for automated email delivery failed updates.
     */
    public static final String EMAIL_VERIFICATION_ENDPOINT_TOKEN = "EMAIL_VERIFICATION_ENDPOINT_TOKEN";

    /**
     * The path for the csv file containing the list of schools.
     */
    public static final String SCHOOL_CSV_LIST_PATH = "SCHOOL_CSV_LIST_PATH";

    // GIT stuff
    public static final String LOCAL_GIT_DB = "LOCAL_GIT_DB";
    public static final String REMOTE_GIT_SSH_KEY_PATH = "REMOTE_GIT_SSH_KEY_PATH";
    public static final String REMOTE_GIT_SSH_URL = "REMOTE_GIT_SSH_URL";

    /**
     * Constant representing the key for the configuration option indicating whether or not unpublished content should
     * be visible.
     */
    public static final String SHOW_ONLY_PUBLISHED_CONTENT = "SHOW_ONLY_PUBLISHED_CONTENT";

    /**
     * Constant representing the key for the location for the version config file..
     */
    public static final String CONTENT_INDICES_LOCATION = "CONTENT_INDICES_LOCATION";

    /**
     * Constant representing the segue application version.
     */
    public static final String SEGUE_APP_VERSION = "SEGUE_APP_VERSION";

    // The alias / content index of the 'live' version we should be serving up
    public static final String CONTENT_INDEX = "CONTENT_INDEX";

    // The actual git commit SHA of the content we're using
    public static final String CONTENT_SHA = "CONTENT_SHA";

    /**
     * Constant representing the segue application mode. e.g. either debug or production
     */
    public static final String SEGUE_APP_ENVIRONMENT = "SEGUE_APP_ENVIRONMENT";
    public static final String DEFAULT_LINUX_CONFIG_LOCATION = "/local/data/rutherford/conf/segue-config.properties";

    /**
     * Enum to describe types of server environment / profile.
     */
    public enum EnvironmentType {
        PROD, DEV
    };

    // HMAC stuff
    /**
     * Constant representing the key for the date signed property - used in HMAC calculations.
     */
    public static final String DATE_SIGNED = "DATE_SIGNED";

    /**
     * Constant representing the key for the expiry date property - used in HMAC calculations.
     */
    public static final String DATE_EXPIRES = "DATE_EXPIRES";

    /**
     * Constant representing the key for the additional date property - used in HMAC calculations.
     */
    public static final String PARTIAL_LOGIN_FLAG = "partialLogin";

    /**
     * Constant representing the key for the HMAC property - used in HMAC calculations.
     */
    public static final String HMAC = "HMAC";

    /**
     * Constant representing the property name for the session expiry in seconds when remember me is not set - used in HMAC calculations.
     */
    public static final String SESSION_EXPIRY_SECONDS_DEFAULT = "SESSION_EXPIRY_SECONDS_DEFAULT";

    /**
     * Constant representing the property name for the session expiry in seconds when remember me is set - used in HMAC calculations.
     */
    public static final String SESSION_EXPIRY_SECONDS_REMEMBERED = "SESSION_EXPIRY_SECONDS_REMEMBERED";

    /**
     * Constant representing the key for the SESSION USER ID - used in HMAC calculations.
     */
    public static final String SESSION_USER_ID = "currentUserId";

    /**
     * Constant representing the key for the SESSION TOKEN - used in HMAC calculations.
     */
    public static final String SESSION_TOKEN = "token";

    /**
     * Constant representing the key for the HMAC Salt - used in HMAC calculations.
     */
    public static final String HMAC_SALT = "HMAC_SALT";
    public static final int TRUNCATED_TOKEN_LENGTH = 8;

    // Search stuff
    public static final String SEARCH_CLUSTER_NAME = "SEARCH_CLUSTER_NAME";
    public static final String SEARCH_CLUSTER_ADDRESS = "SEARCH_CLUSTER_ADDRESS";
    public static final String SEARCH_CLUSTER_PORT = "SEARCH_CLUSTER_PORT";
    public static final String SEARCH_CLUSTER_INFO_PORT = "SEARCH_CLUSTER_INFO_PORT";
    public static final String SEARCH_RESULTS_HARD_LIMIT = "SEARCH_RESULTS_HARD_LIMIT";

    // Event management stuff:
    public static final String EVENT_ADMIN_EMAIL = "EVENT_ADMIN_EMAIL";
    public static final String EVENT_ICAL_UID_DOMAIN = "EVENT_ICAL_UID_DOMAIN";

    // MailJet Stuff:
    public static final String MAILJET_WEBHOOK_TOKEN = "MAILJET_WEBHOOK_TOKEN";
    public static final String MAILJET_API_KEY = "MAILJET_API_KEY";
    public static final String MAILJET_API_SECRET = "MAILJET_API_SECRET";
    public static final String MAILJET_NEWS_LIST_ID = "MAILJET_NEWS_LIST_ID";
    public static final String MAILJET_EVENTS_LIST_ID = "MAILJET_EVENTS_LIST_ID";

    /**
     * Suffix to append to raw fields (minus dot separator) - these are fields that the search engine should not do any
     * processing on (e.g. no stemming)
     */
    public static final String UNPROCESSED_SEARCH_FIELD_SUFFIX = "raw";

    /**
     * Enum to represent sort orders.
     * 
     */
    public enum SortOrder {
        ASC, DESC
    };

    /**
     * Enum to represent search boolean operators.
     * 
     */
    public enum BooleanOperator {
        AND, OR, NOT
    };

    public static final String SCHOOLS_INDEX_BASE = "schools";
    public enum SCHOOLS_INDEX_TYPE {
        METADATA("metadata"),
        SCHOOL_SEARCH("school");

        private String typeName;

        SCHOOLS_INDEX_TYPE(final String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return this.typeName;
        }
    }

    public enum CONTENT_INDEX_TYPE {
        METADATA("metadata"),
        UNIT("unit"),
        PUBLISHED_UNIT("publishedUnit"),
        CONTENT("content"),
        CONTENT_ERROR("contentError");

        private String typeName;

        CONTENT_INDEX_TYPE(final String typeName) {
            this.typeName = typeName;
        }

        @Override
        public String toString() {
            return this.typeName;
        }
    }

    // Federated Authentication Stuff
    /**
     * This constant will be used to determine if we are expecting a link account request or not.
     */
    public static final String LINK_ACCOUNT_PARAM_NAME = "LINK_ACCOUNT_PARAM_NAME";

    /**
     * This constant is used for matching against url params to prevent CSRF.
     */
    public static final String STATE_PARAM_NAME = "state";

    /**
     * This constant is used for matching against url params to prevent CSRF.
     */
    public static final String OAUTH_TOKEN_PARAM_NAME = "oauth_token";

    // Google properties
    public static final String GOOGLE_CLIENT_SECRET_LOCATION = "GOOGLE_CLIENT_SECRET_LOCATION";
    public static final String GOOGLE_CALLBACK_URI = "GOOGLE_CALLBACK_URI";
    public static final String GOOGLE_OAUTH_SCOPES = "GOOGLE_OAUTH_SCOPES";

    // Facebook properties
    public static final String FACEBOOK_SECRET = "FACEBOOK_SECRET";
    public static final String FACEBOOK_CLIENT_ID = "FACEBOOK_CLIENT_ID";
    public static final String FACEBOOK_CALLBACK_URI = "FACEBOOK_CALLBACK_URI";
    public static final String FACEBOOK_OAUTH_SCOPES = "FACEBOOK_OAUTH_SCOPES";
    public static final String FACEBOOK_USER_FIELDS = "FACEBOOK_USER_FIELDS";

    // Twitter properties
    public static final String TWITTER_SECRET = "TWITTER_SECRET";
    public static final String TWITTER_CLIENT_ID = "TWITTER_CLIENT_ID";
    public static final String TWITTER_CALLBACK_URI = "TWITTER_CALLBACK_URI";

    // Local authentication specific stuff
    public static final String LOCAL_AUTH_EMAIL_FIELDNAME = "email";
    public static final String LOCAL_AUTH_EMAIL_VERIFICATION_TOKEN_FIELDNAME = "emailVerificationToken";
    public static final String LOCAL_AUTH_GROUP_MANAGER_INITIATED_FIELDNAME = "groupManagerInitiated";
    public static final String LOCAL_AUTH_GROUP_MANAGER_EMAIL_FIELDNAME = "groupManagerEmail";

    // Database properties
    public static final String SEGUE_DB_NAME = "SEGUE_DB_NAME";

    public static final String POSTGRES_DB_URL = "POSTGRES_DB_URL";
    public static final String POSTGRES_DB_USER = "POSTGRES_DB_USER";
    public static final String POSTGRES_DB_PASSWORD = "POSTGRES_DB_PASSWORD";

    public enum TimeInterval {
        SIX_MONTHS(0, 6, 0, 0, 0, 0),
        NINETY_DAYS(0, 0, 90, 0, 0, 0),
        THIRTY_DAYS(0, 0, 30, 0, 0, 0),
        SEVEN_DAYS(0, 0, 7, 0, 0, 0);

        private final PGInterval interval;

        TimeInterval(int years, int months, int days, int hours, int minutes, double seconds) {
            this.interval = new PGInterval(years, months, days, hours, minutes, seconds);
        }
        public PGInterval getPGInterval() {
            return this.interval;
        }
    }

    // Logging component
    public static final String LOGGING_ENABLED = "LOGGING_ENABLED";
    public static final Integer MAX_LOG_REQUEST_BODY_SIZE_IN_BYTES = 1000000;

    public interface LogType {
        /**
         * Get the string value of the log Enum.
         * @return name of the log type
         */
        String name();
    }

    /**
     *  Class to represent Segue Log Types.
     */
    public enum SegueServerLogType implements LogType {
        ADD_ADDITIONAL_GROUP_MANAGER,
        ADMIN_CHANGE_USER_SCHOOL,
        ADMIN_EVENT_ATTENDANCE_RECORDED,
        ADMIN_EVENT_BOOKING_CANCELLED,
        ADMIN_EVENT_BOOKING_CREATED,
        ADMIN_EVENT_BOOKING_DELETED,
        ADMIN_EVENT_WAITING_LIST_PROMOTION,
        ADMIN_MERGE_USER,
        ANSWER_QUESTION,
        ANSWER_QUIZ_QUESTION,
        CHANGE_USER_ROLE,
        CHANGE_GROUP_MEMBERSHIP_STATUS,
        CONTACT_US_FORM_USED,
        CREATE_USER_ASSOCIATION,
        CREATE_USER_GROUP,
        DELETE_ADDITIONAL_GROUP_MANAGER,
        DELETE_USER_ACCOUNT,
        DELETE_USER_GROUP,
        EMAIL_VERIFICATION_REQUEST_RECEIVED,
        EVENT_BOOKING,
        EVENT_BOOKING_CANCELLED,
        EVENT_WAITING_LIST_BOOKING,
        EVENT_RESERVATIONS_CREATED,
        EVENT_RESERVATIONS_CANCELLED,
        LOG_IN,
        LOG_OUT,
        LOG_OUT_EVERYWHERE,
        MERGE_USER,
        PASSWORD_RESET_REQUEST_RECEIVED,
        PASSWORD_RESET_REQUEST_SUCCESSFUL,
        QUESTION_ATTEMPT_RATE_LIMITED,
        RELEASE_USER_ASSOCIATION,
        REMOVE_USER_FROM_GROUP,
        REVOKE_USER_ASSOCIATION,
        SEND_MASS_EMAIL,
        SENT_EMAIL,
        USER_REGISTRATION,
        USER_SCHOOL_CHANGE
    }

    public static final Set<String> SEGUE_SERVER_LOG_TYPES = Arrays.stream(SegueServerLogType.values()).map(SegueServerLogType::name).collect(Collectors.toSet());

    // Websocket Component
    public static final String MAX_CONCURRENT_WEB_SOCKETS_PER_USER = "MAX_CONCURRENT_WEB_SOCKETS_PER_USER";

    // Metrics Component
    public static final String API_METRICS_EXPORT_PORT = "API_METRICS_EXPORT_PORT";

    // IP Geocoding stuff
    public static final String IP_INFO_DB_API_KEY = "IP_INFO_DB_API_KEY";


    /*
     * Default values.
     */
    public static final Integer DEFAULT_START_INDEX = 0;
    public static final String DEFAULT_START_INDEX_AS_STRING = "0";
    public static final String DEFAULT_TYPE_FILTER = "";

    public static final Integer MAX_NOTE_CHAR_LENGTH = 500;

    public static final Integer DEFAULT_RESULTS_LIMIT = 10;
    public static final String DEFAULT_RESULTS_LIMIT_AS_STRING = "10";

    public static final String DEFAULT_SEARCH_RESULT_LIMIT_AS_STRING = "25";

    public static final Integer SEARCH_TEXT_CHAR_LIMIT = 1000;

    public static final Integer NO_SEARCH_LIMIT = -1;

    // Content model specific stuff
    public static final String ID_FIELDNAME = "id";
    public static final String TITLE_FIELDNAME = "title";
    public static final String TYPE_FIELDNAME = "type";
    public static final String TAGS_FIELDNAME = "tags";
    public static final String VALUE_FIELDNAME = "value";
    public static final String LEVEL_FIELDNAME = "level";
    public static final String SUMMARY_FIELDNAME = "summary";
    public static final String CHILDREN_FIELDNAME = "children";
    public static final String DATE_FIELDNAME = "date";
    public static final String[] ADDRESS_PATH_FIELDNAME = {"location", "address"};
    public static final String[] ADDRESS_FIELDNAMES = {"addressLine1", "addressLine2", "town", "county", "postalCode"};
    public static final String SEARCHABLE_CONTENT_FIELDNAME = "searchableContent";
    public static final String VISIBLE_TO_STUDENTS_FIELDNAME = "visibleToStudents";
    public static final String QUIZ_RUBRIC_FIELDNAME = "rubric";

    public static final String STAGE_FIELDNAME = "audience.stage";
    public static final String DIFFICULTY_FIELDNAME = "audience.difficulty";
    public static final String EXAM_BOARD_FIELDNAME = "audience.examBoard";
    public static final Set<String> NESTED_FIELDS =
            ImmutableSet.of(STAGE_FIELDNAME, DIFFICULTY_FIELDNAME, EXAM_BOARD_FIELDNAME);

    public static final String USER_ID_FKEY_FIELDNAME = "userId";
    public static final String OLD_USER_ID_FKEY_FIELDNAME = "oldUserId";
    public static final String USER_ID_LIST_FKEY_FIELDNAME = "userIds";
    public static final String EVENT_ID_FKEY_FIELDNAME = "eventId";
    public static final String BOOKING_STATUS_FIELDNAME = "bookingStatus";
    public static final String ADMIN_BOOKING_REASON_FIELDNAME = "authorisationReason";
    public static final String ATTENDED_FIELDNAME = "attended";
    public static final String EVENT_DATE_FIELDNAME = "eventDate";
    public static final String EVENT_TAGS_FIELDNAME = "eventTags";
    public static final String CONTENT_VERSION_FIELDNAME = "contentVersion";

    public static final int EVENT_DATE_EPOCH_MULTIPLIER = 1000;
    public static final Integer EVENT_RESERVATION_CLOSE_INTERVAL_DAYS = 14;
    public static final Integer EVENT_GROUP_RESERVATION_DEFAULT_LIMIT = 10;

    /**
     *  Enum to represent filter values for event management.
     */
    public enum EventFilterOption {
        FUTURE, RECENT, PAST
    }

    public static final String ID_SEPARATOR = "|";
    public static final String ESCAPED_ID_SEPARATOR = "\\" + ID_SEPARATOR;

    // School List loading - raw data
    public static final String SCHOOL_URN_FIELDNAME = "URN";
    public static final String SCHOOL_ESTABLISHMENT_NAME_FIELDNAME = "EstablishmentName";
    public static final String SCHOOL_POSTCODE_FIELDNAME = "Postcode";
    public static final String SCHOOL_DATA_SOURCE_FIELDNAME = "DataSource";
    public static final String SCHOOL_CLOSED_FIELDNAME = "Closed";

    // School List loading POJO fields
    public static final String SCHOOL_URN_FIELDNAME_POJO = "urn";
    public static final String SCHOOL_ESTABLISHMENT_NAME_FIELDNAME_POJO = "name";
    public static final String SCHOOL_POSTCODE_FIELDNAME_POJO = "postcode";

    // User School Reporting

    /**
     *  Represent the information a user has provided about their school status.
     */
    public enum SchoolInfoStatus {
        PROVIDED, OTHER_PROVIDED, BOTH_PROVIDED, NOT_PROVIDED;

        /**
         *  Return the status given the state of the two school fields
         * @param schoolIdProvided - whether a school_id is provided
         * @param schoolOtherProvided - whether a school_other is provided
         * @return
         */
        public static SchoolInfoStatus get(final boolean schoolIdProvided, final boolean schoolOtherProvided) {
            if (schoolIdProvided && schoolOtherProvided) {
                return BOTH_PROVIDED;
            } else if (schoolIdProvided) {
                return PROVIDED;
            } else if (schoolOtherProvided) {
                return OTHER_PROVIDED;
            }
            return NOT_PROVIDED;
        }
    }

    // cache settings
    public static final String MAX_CONTENT_CACHE_TIME = "MAX_CONTENT_CACHE_TIME";
    
    public static final int NUMBER_SECONDS_IN_MINUTE = 60;
    public static final int NUMBER_SECONDS_IN_FIVE_MINUTES = NUMBER_SECONDS_IN_MINUTE * 5;
    public static final int NUMBER_SECONDS_IN_TEN_MINUTES = NUMBER_SECONDS_IN_MINUTE * 10;
    public static final int NUMBER_SECONDS_IN_FIFTEEN_MINUTES = NUMBER_SECONDS_IN_MINUTE * 15;
    public static final int NUMBER_SECONDS_IN_ONE_HOUR = NUMBER_SECONDS_IN_MINUTE * 60;
    public static final int NUMBER_SECONDS_IN_ONE_DAY = NUMBER_SECONDS_IN_ONE_HOUR * 24;
    public static final int NUMBER_SECONDS_IN_ONE_WEEK = NUMBER_SECONDS_IN_ONE_DAY * 7;
    public static final int NUMBER_SECONDS_IN_THIRTY_DAYS = NUMBER_SECONDS_IN_ONE_DAY * 30;
    public static final int ANONYMOUS_SESSION_DURATION_IN_MINUTES = 40;
    public static final int NEVER_CACHE_WITHOUT_ETAG_CHECK = 0;

    public static final String ANONYMOUS_USER = "ANONYMOUS_USER";
    public static final int LAST_SEEN_UPDATE_FREQUENCY_MINUTES = 5;

    /**
     * Redirect response field name.
     */
    public static final String REDIRECT_URL = "redirectUrl";

    public static final String SEGUE_AUTH_COOKIE = "SEGUE_AUTH_COOKIE";
    public static final String JSESSION_COOOKIE = "JSESSIONID";

    public static final String DEFAULT_DATE_FORMAT = "EEE MMM dd HH:mm:ss Z yyyy";

    public static final String ASSOCIATION_TOKEN_FIELDNAME = "token";

    public static final String GROUP_FK = "groupId";
    public static final String ASSIGNMENT_FK = "assignmentId";
    public static final String ASSIGNMENT_DUEDATE_FK = "dueDate";

    public static final String QUIZ_ATTEMPT_FK = "quizAttemptId";
    public static final String QUIZ_ASSIGNMENT_FK = "quizAssignmentId";

    public static final String EQUALITY_CHECKER_HOST = "EQUALITY_CHECKER_HOST";
    public static final String EQUALITY_CHECKER_PORT = "EQUALITY_CHECKER_PORT";

    public static final String CHEMISTRY_CHECKER_HOST = "CHEMISTRY_CHECKER_HOST";
    public static final String CHEMISTRY_CHECKER_PORT = "CHEMISTRY_CHECKER_PORT";

    public static final String QUESTION_MISUSE_THRESHOLD_OVERRIDE = "QUESTION_MISUSE_THRESHOLD_OVERRIDE";

    // User Preferences:
    public enum SegueUserPreferences {
        EMAIL_PREFERENCE
    }

    /**
     * Private constructor to prevent this class being created.
     */
    private Constants() {
        // not allowed to create one of these as it wouldn't make sense.
    }
}
