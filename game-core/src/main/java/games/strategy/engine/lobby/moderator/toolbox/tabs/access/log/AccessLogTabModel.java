package games.strategy.engine.lobby.moderator.toolbox.tabs.access.log;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.triplea.http.client.lobby.moderator.toolbox.PagingParams;
import org.triplea.http.client.lobby.moderator.toolbox.banned.name.ToolboxUsernameBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.http.client.lobby.moderator.toolbox.log.ToolboxAccessLogClient;

@RequiredArgsConstructor
class AccessLogTabModel {
  private final ToolboxAccessLogClient toolboxAccessLogClient;
  private final ToolboxUserBanClient toolboxUserBanClient;
  private final ToolboxUsernameBanClient toolboxUsernameBanClient;

  static List<String> fetchTableHeaders() {
    return List.of("Access Date", "Username", "IP", "System Id", "Registered", "", "");
  }

  List<List<String>> fetchTableData(final PagingParams pagingParams) {
    return toolboxAccessLogClient.getAccessLog(pagingParams).stream()
        .map(
            accessLogData ->
                List.of(
                    accessLogData.getAccessDate().toString(),
                    accessLogData.getUsername(),
                    accessLogData.getIp(),
                    accessLogData.getSystemId(),
                    accessLogData.isRegistered() ? "Y" : "",
                    "Ban Name",
                    "Ban User"))
        .collect(Collectors.toList());
  }

  void banUserName(final String username) {
    toolboxUsernameBanClient.addUsernameBan(username);
  }

  void banUser(final UserBanParams banUserParams) {
    toolboxUserBanClient.banUser(banUserParams);
  }
}
