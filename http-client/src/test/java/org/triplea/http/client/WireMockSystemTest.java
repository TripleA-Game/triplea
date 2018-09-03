package org.triplea.http.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.triplea.http.data.error.report.ErrorReport;
import org.triplea.http.data.error.report.ErrorReportDetails;
import org.triplea.http.data.error.report.ErrorReportResponse;
import org.triplea.test.common.Integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;

import ru.lanwen.wiremock.ext.WiremockResolver;
import ru.lanwen.wiremock.ext.WiremockUriResolver;

/**
 * A test that checks the http client works, we use wiremock to simulate a server so we are not coupled
 * to any one server implementation. Server sub-projects should include the http-client as a test dependency
 * to then create an integration test to be sure that everything would work. Meanwhile we can test here
 * against a generic/stubbed server to be sure the client contract works as expected.
 */
@ExtendWith({
    WiremockResolver.class,
    WiremockUriResolver.class
})
@Integration
class WireMockSystemTest {
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String MESSAGE_FROM_USER = "msg";
  private static final String GAME_VERSION = "version";
  private static final LogRecord logRecord = new LogRecord(Level.SEVERE, "record");
  private static final int TIMEOUT_MILLIS = 200;
  private static final int SHORT_TIMEOUT_MILLIS = 5;

  @Test
  void sendErrorReportSuccessCase(
      @WiremockResolver.Wiremock final WireMockServer server,
      @WiremockUriResolver.WiremockUri final String uri) {

    givenHttpServerSuccessResponse(server);

    final ServiceCallResult<ErrorReportResponse> response = doServiceCall(server);

    verify(postRequestedFor(urlMatching(ErrorReportingHttpClient.ERROR_REPORT_PATH))
        .withRequestBody(containing(MESSAGE_FROM_USER))
        .withRequestBody(containing(GAME_VERSION))
        .withRequestBody(containing(logRecord.getMessage()))
        .withRequestBody(containing(logRecord.getLevel().toString()))
        .withHeader(HttpHeaders.CONTENT_TYPE, matching(CONTENT_TYPE_JSON)));

    assertThat(response.getThrown().isPresent(), is(false));
    assertThat(response.getPayload().isPresent(), is(true));
  }

  private static void givenHttpServerSuccessResponse(final WireMockServer wireMockServer) {
    wireMockServer.stubFor(post(urlEqualTo(ErrorReportingHttpClient.ERROR_REPORT_PATH))
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody(String.format("{ \"result\":\"%s\" }", ErrorReportResponse.SUCCESS))));

  }

  private ServiceCallResult<ErrorReportResponse> doServiceCall(final WireMockServer wireMockServer) {
    return doServiceCall(wireMockServer, TIMEOUT_MILLIS);
  }

  private ServiceCallResult<ErrorReportResponse> doServiceCall(
      final WireMockServer wireMockServer,
      final int timeoutMillis) {

    WireMock.configureFor("localhost", wireMockServer.port());
    final URI hostUri = URI.create(wireMockServer.url(""));
    return new ErrorReportingClient(
        ErrorReportingHttpClient.newClient(hostUri, timeoutMillis, timeoutMillis),
        ErrorReport::new,
        Collections.emptyList())
        .sendErrorReport(ErrorReportDetails.builder()
            .messageFromUser(MESSAGE_FROM_USER)
            .gameVersion(GAME_VERSION)
            .logRecord(logRecord)
            .build());
  }

  @Test
  void communicationFaultCases(
      @WiremockResolver.Wiremock final WireMockServer wireMockServer,
      @WiremockUriResolver.WiremockUri final String uri) {

    Arrays.asList(
        // caution, one of the wiremock faults is known to cause a hang in windows, so to aviod that
        // problem do not use the full available list of of wiremock faults
        Fault.EMPTY_RESPONSE,
        Fault.RANDOM_DATA_THEN_CLOSE)
        .forEach(fault -> testFaultHandling(wireMockServer, fault));
  }

  private void testFaultHandling(final WireMockServer wireMockServer, final Fault fault) {
    givenFaultyConnection(wireMockServer, fault);

    final ServiceCallResult<ErrorReportResponse> response = doServiceCall(wireMockServer);

    assertThat(response.getPayload().isPresent(), is(false));
    assertThat(response.getThrown().isPresent(), is(true));
    assertThat(response.getErrorDetails(), not(emptyOrNullString()));
  }

  private static void givenFaultyConnection(final WireMockServer wireMockServer, final Fault fault) {
    wireMockServer.stubFor(post(urlEqualTo(ErrorReportingHttpClient.ERROR_REPORT_PATH))
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withFault(fault)
            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody("a simulated error occurred")));
  }

  @Test
  void server500(
      @WiremockResolver.Wiremock final WireMockServer wireMockServer,
      @WiremockUriResolver.WiremockUri final String uri) {
    givenServer500(wireMockServer);

    final ServiceCallResult<ErrorReportResponse> response = doServiceCall(wireMockServer);

    assertThat(response.getPayload().isPresent(), is(false));
    assertThat(response.getThrown().isPresent(), is(true));
    assertThat(response.getErrorDetails(), not(emptyOrNullString()));
  }

  private static void givenServer500(final WireMockServer wireMockServer) {
    wireMockServer.stubFor(post(urlEqualTo(ErrorReportingHttpClient.ERROR_REPORT_PATH))
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody("{ \"result\":\"FAILURE\" }")));
  }

  @Test
  void timeoutCase(
      @WiremockResolver.Wiremock final WireMockServer wireMockServer,
      @WiremockUriResolver.WiremockUri final String uri) {
    WireMock.configureFor("localhost", wireMockServer.port());

    final int delayGreaterThanTheTimeout = SHORT_TIMEOUT_MILLIS + 5;

    wireMockServer.stubFor(post(urlEqualTo(ErrorReportingHttpClient.ERROR_REPORT_PATH))
        .withHeader(HttpHeaders.ACCEPT, equalTo(CONTENT_TYPE_JSON))
        .willReturn(aResponse()
            .withFixedDelay(delayGreaterThanTheTimeout)
            .withStatus(HttpStatus.SC_OK)
            .withHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON)
            .withBody("{ \"result\":\"SUCCESS\" }")));

    final ServiceCallResult<ErrorReportResponse> response = doServiceCall(wireMockServer, SHORT_TIMEOUT_MILLIS);

    assertThat(response.getPayload().isPresent(), is(false));
    assertThat(response.getThrown().isPresent(), is(true));
  }
}
