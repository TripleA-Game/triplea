package games.strategy.engine.lobby.moderator.toolbox.tabs;

import java.util.Arrays;
import java.util.List;

import lombok.AllArgsConstructor;
import org.triplea.http.client.moderator.toolbox.ModeratorToolboxClient;


@AllArgsConstructor
public class EventLogTabModel {
  private final ModeratorToolboxClient moderatorToolboxClient;

  static List<String> getEventLogTableHeaders() {
    return Arrays.asList("Date", "Moderator", "Action", "Value");
  }

  static List<List<String>> getEventLogTableData() {
    return Arrays.asList(
        Arrays.asList("Jan", "Champ", "Banned", "Troll"),
        Arrays.asList("Feb", "Prastle", "Tased", "Pest"));
  }

  static List<List<String>> loadMore() {
    return Arrays.asList(
        Arrays.asList("March", "Moderator", "Loded More", "Data"),
        Arrays.asList("Dec", "Mod2", "Loded Moar", "Data"));
  }

}
