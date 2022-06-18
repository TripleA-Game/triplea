package org.triplea.ai.does.nothing;

import games.strategy.engine.framework.startup.ui.PlayerTypes;
import games.strategy.triplea.ai.AbstractAi;
import games.strategy.triplea.ai.AiProvider;

public class DoesNothingAiProvider implements AiProvider {
  @Override
  public AbstractAi create(final String name, final String playerLabel) {
    return new DoesNothingAi(name, playerLabel);
  }

  @Override
  public String getLabel() {
    return PlayerTypes.DOES_NOTHING_PLAYER_LABEL;
  }
}
