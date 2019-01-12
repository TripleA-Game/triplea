package org.triplea.game.common;

import games.strategy.engine.chat.Chat;

/**
 * Interface to abstract common functionality to configure a chat
 * that is shared between headed and headless implementations.
 */
public interface ChatConfiguration {

  void setChat(final Chat chat);

  Chat getChat();

  String getAllText();

  void setShowChatTime(final boolean showTime);
}
