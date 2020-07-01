package org.triplea.debug.error.reporting;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.system.SystemProperties;
import java.util.Arrays;
import java.util.logging.LogRecord;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.error.report.ErrorReportRequest;

@ExtendWith(MockitoExtension.class)
class StackTraceErrorReportFormatterTest {
  private static final String SAMPLE_USER_DESCRIPTION = "Pol, a bene vortex";
  private static final String LOG_MESSAGE =
      "LogMessage, Golly gosh, yer not drinking me without a desolation!";

  private static final String CLASS_SHORT_NAME = "ClassShortName";
  private static final String CLASS_NAME = "org.triplea." + CLASS_SHORT_NAME;
  private static final String METHOD_NAME = "methodParrot";
  private static final Exception EXCEPTION_WITH_MESSAGE =
      new RuntimeException("simulated exception");
  private static final Exception EXCEPTION_WITH_NO_MESSAGE = new NullPointerException();
  private static final Exception EXCEPTION_WITH_CAUSE =
      new RuntimeException(EXCEPTION_WITH_MESSAGE);

  @Mock private LogRecord logRecord;

  @Nested
  final class VerifyVersion {
    @Test
    void verifyVersion() {
      when(logRecord.getSourceClassName()).thenReturn("org.ClassName");

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter(() -> "4.1")
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(errorReportResult.getGameVersion(), is("4.1"));
    }
  }

  @Nested
  final class VerifyTitle {
    @Test
    void logMessageOnly() {
      when(logRecord.getSourceClassName()).thenReturn("org.ClassName");
      when(logRecord.getSourceMethodName()).thenReturn(METHOD_NAME);
      when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter(() -> "4.1")
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(
          errorReportResult.getTitle(), is("ClassName#" + METHOD_NAME + " - " + LOG_MESSAGE));
    }

    @Test
    void logMessageOnlyWithTripleaPackage() {
      when(logRecord.getSourceClassName()).thenReturn("org.triplea.ClassName");
      when(logRecord.getSourceMethodName()).thenReturn(METHOD_NAME);
      when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter(() -> "4.1")
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(
          errorReportResult.getTitle(), is("ClassName#" + METHOD_NAME + " - " + LOG_MESSAGE));
    }

    @Test
    void handleNullLogMessageAndNullExceptionMessage() {
      when(logRecord.getThrown()).thenReturn(EXCEPTION_WITH_NO_MESSAGE);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter(() -> "5.6")
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(
          errorReportResult.getTitle(),
          is(
              StackTraceErrorReportFormatterTest.class.getSimpleName()
                  + "#<clinit>:"
                  + EXCEPTION_WITH_NO_MESSAGE.getStackTrace()[0].getLineNumber()
                  + " - "
                  + EXCEPTION_WITH_NO_MESSAGE.getClass().getSimpleName()));
    }

    @Test
    void handleClassNameWithNoPackages() {
      when(logRecord.getSourceMethodName()).thenReturn(METHOD_NAME);
      when(logRecord.getSourceClassName()).thenReturn("ClassInDefaultPackage");

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter()
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertDoesNotThrow(errorReportResult::getTitle);
    }

    @Test
    void handleExceptionWithCause() {
      when(logRecord.getThrown()).thenReturn(EXCEPTION_WITH_CAUSE);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter(() -> "5.6")
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(
          errorReportResult.getTitle(),
          is(
              StackTraceErrorReportFormatterTest.class.getSimpleName()
                  + "#<clinit>:"
                  + EXCEPTION_WITH_CAUSE.getCause().getStackTrace()[0].getLineNumber()
                  + " - "
                  + EXCEPTION_WITH_CAUSE.getCause().getClass().getSimpleName()));
    }
  }

  @Nested
  final class VerifyBodyContains {
    @Test
    void containsUseSuppliedData() {
      when(logRecord.getSourceClassName()).thenReturn(CLASS_NAME);
      when(logRecord.getSourceMethodName()).thenReturn(METHOD_NAME);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter()
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(errorReportResult.getBody(), containsString(SAMPLE_USER_DESCRIPTION));
    }

    @Test
    void containsMapName() {
      when(logRecord.getSourceClassName()).thenReturn(CLASS_NAME);
      when(logRecord.getSourceMethodName()).thenReturn(METHOD_NAME);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter()
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(errorReportResult.getBody(), containsString("mapName"));
    }

    @Test
    void containsSystemData() {
      when(logRecord.getSourceClassName()).thenReturn(CLASS_NAME);
      when(logRecord.getSourceMethodName()).thenReturn(METHOD_NAME);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter()
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(errorReportResult.getBody(), containsString(SAMPLE_USER_DESCRIPTION));
      assertThat(
          errorReportResult.getBody(), containsString(SystemProperties.getOperatingSystem()));
      assertThat(errorReportResult.getBody(), containsString(SystemProperties.getJavaVersion()));
      assertThat(
          errorReportResult.getBody(), containsString(ClientContext.engineVersion().toString()));
    }

    @Test
    void containsStackTraceData() {
      when(logRecord.getThrown()).thenReturn(EXCEPTION_WITH_CAUSE);
      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter()
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      Stream.of(EXCEPTION_WITH_CAUSE, EXCEPTION_WITH_MESSAGE)
          .map(Throwable::getStackTrace)
          .flatMap(Arrays::stream)
          .forEach(
              trace ->
                  assertThat(
                      "should contain each element of stack trace",
                      errorReportResult.getBody(),
                      containsString(trace.toString())));

      assertThat(
          "should contain message of cause",
          errorReportResult.getBody(),
          containsString(EXCEPTION_WITH_MESSAGE.getMessage()));
    }

    @Test
    void containsExceptionMessageAndLogMessageWhenBothArePresent() {
      when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);
      when(logRecord.getThrown()).thenReturn(EXCEPTION_WITH_MESSAGE);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter()
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(
          errorReportResult.getBody(), containsString(EXCEPTION_WITH_MESSAGE.getClass().getName()));
      assertThat(errorReportResult.getBody(), containsString(LOG_MESSAGE));
    }
  }

  @Nested
  final class BodyNullMessageHandling {
    @Test
    void nullLogMessage() {
      when(logRecord.getThrown()).thenReturn(EXCEPTION_WITH_NO_MESSAGE);

      final ErrorReportRequest errorReportResult =
          new StackTraceErrorReportFormatter()
              .apply(
                  ErrorReportRequestParams.builder()
                      .userDescription(SAMPLE_USER_DESCRIPTION)
                      .mapName("mapName")
                      .logRecord(logRecord)
                      .build());

      assertThat(
          errorReportResult.getBody(),
          containsString(EXCEPTION_WITH_NO_MESSAGE.getClass().getName()));
    }
  }
}
