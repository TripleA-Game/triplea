package org.triplea.http.client.moderator.toolbox.access.log;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.moderator.toolbox.PagingParams;

/**
 * Http client class for fetching rows of the access log table. That is a table noting users that
 * have accessed the lobby. The data is useful for informative reasons and provides the key
 * parameters for adding user name or user bans.
 */
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ToolboxAccessLogClient {
  public static final String FETCH_ACCESS_LOG_PATH = "/moderator-toolbox/get-access-log";

  private final AuthenticationHeaders authenticationHeaders;
  private final ToolboxAccessLogFeignClient client;

  public static ToolboxAccessLogClient newClient(final URI serverUri, final String apiKey) {
    return new ToolboxAccessLogClient(
        new AuthenticationHeaders(apiKey),
        new HttpClient<>(ToolboxAccessLogFeignClient.class, serverUri).get());
  }

  public List<AccessLogData> getAccessLog(final PagingParams pagingParams) {
    checkArgument(pagingParams.getRowNumber() >= 0);
    checkArgument(pagingParams.getPageSize() > 0);
    return client.getAccessLog(authenticationHeaders.createHeaders(), pagingParams);
  }
}
