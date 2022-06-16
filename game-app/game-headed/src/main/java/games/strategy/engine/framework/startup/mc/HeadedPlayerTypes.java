package games.strategy.engine.framework.startup.mc;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.ai.AiProvider;
import games.strategy.triplea.settings.ClientSetting;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.triplea.ai.does.nothing.DoesNothingAiProvider;
import org.triplea.ai.flowfield.FlowFieldAiProvider;

public class HeadedPlayerTypes {
  private static boolean filterBetaPlayerType(final PlayerTypes.Type playerType) {
    if (playerType.getLabel().equals("FlowField (AI)")) {
      return ClientSetting.showBetaFeatures.getValue().orElse(false);
    }
    return true;
  }

  public static Collection<PlayerTypes.Type> getPlayerTypes() {
    return Stream.of(
            PlayerTypes.getBuiltInPlayerTypes(),
            List.of(
                new PlayerTypes.AiType(new DoesNothingAiProvider()),
                new PlayerTypes.AiType(new FlowFieldAiProvider())),
            StreamSupport.stream(ServiceLoader.load(AiProvider.class).spliterator(), false)
                .map(PlayerTypes.AiType::new)
                .collect(Collectors.toSet()))
        .flatMap(Collection::stream)
        .filter(HeadedPlayerTypes::filterBetaPlayerType)
        .collect(Collectors.toList());
  }
}
