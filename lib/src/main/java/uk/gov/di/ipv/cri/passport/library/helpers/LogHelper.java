package uk.gov.di.ipv.cri.passport.library.helpers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.lambda.powertools.logging.LoggingUtils;
import uk.gov.di.ipv.cri.passport.library.annotations.ExcludeFromGeneratedCoverageReport;

@ExcludeFromGeneratedCoverageReport
public class LogHelper {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final String COMPONENT_ID = "passport-cri";

    public enum LogField {
        CLIENT_ID_LOG_FIELD("clientId"),
        COMPONENT_ID_LOG_FIELD("componentId"),
        ERROR_CODE_LOG_FIELD("errorCode"),
        ERROR_DESCRIPTION_LOG_FIELD("errorDescription"),
        PASSPORT_SESSION_ID_LOG_FIELD("passportSessionId");

        private final String fieldName;

        LogField(String fieldName) {
            this.fieldName = fieldName;
        }

        String getFieldName() {
            return this.fieldName;
        }
    }

    private LogHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static void attachComponentIdToLogs() {
        attachFieldToLogs(LogField.COMPONENT_ID_LOG_FIELD, COMPONENT_ID);
    }

    public static void attachClientIdToLogs(String clientId) {
        attachFieldToLogs(LogField.CLIENT_ID_LOG_FIELD, clientId);
    }

    public static void attachPassportSessionIdToLogs(String sessionId) {
        attachFieldToLogs(LogField.PASSPORT_SESSION_ID_LOG_FIELD, sessionId);
    }

    public static void logOauthError(String message, String errorCode, String errorDescription) {
        LoggingUtils.appendKey(LogField.ERROR_CODE_LOG_FIELD.getFieldName(), errorCode);
        LoggingUtils.appendKey(
                LogField.ERROR_DESCRIPTION_LOG_FIELD.getFieldName(), errorDescription);
        LOGGER.error(message);
        LoggingUtils.removeKeys(
                LogField.ERROR_CODE_LOG_FIELD.getFieldName(),
                LogField.ERROR_DESCRIPTION_LOG_FIELD.getFieldName());
    }

    private static void attachFieldToLogs(LogField field, String value) {
        LoggingUtils.appendKey(field.getFieldName(), value);
        LOGGER.info("{} attached to logs", field);
    }
}
