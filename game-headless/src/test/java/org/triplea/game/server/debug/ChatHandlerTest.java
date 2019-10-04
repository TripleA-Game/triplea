package org.triplea.game.server.debug;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class ChatHandlerTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class PublishTest {
    private final ChatHandler chatHandler = new ChatHandler();
    @Mock private Consumer<String> sendChatMessage;

    private LogRecord newLoggableLogRecord() {
      return new LogRecord(Level.WARNING, "message");
    }

    private LogRecord newUnloggableLogRecord() {
      return new LogRecord(Level.FINEST, "message");
    }

    private void publish(final LogRecord record) {
      synchronized (chatHandler) {
        chatHandler.publish(record, sendChatMessage);
      }
    }

    @Test
    void shouldSendChatMessageWhenRecordIsLoggable() {
      publish(newLoggableLogRecord());

      verify(sendChatMessage).accept(anyString());
    }

    @Test
    void shouldNotSendChatMessageWhenRecordIsNotLoggable() {
      publish(newUnloggableLogRecord());

      verify(sendChatMessage, never()).accept(anyString());
    }

    @Test
    void shouldNotIncludeTrailingNewlineInChatMessage() {
      publish(newLoggableLogRecord());

      verify(sendChatMessage).accept(not(endsWith("\n")));
    }

    @Test
    void shouldNotSendChatMessageWhenRecordIsLoggableAndCallIsReentrant() {
      doAnswer(
              invocation -> {
                publish(newLoggableLogRecord());
                return null;
              })
          .when(sendChatMessage)
          .accept(anyString());

      publish(newLoggableLogRecord());

      verify(sendChatMessage, times(1)).accept(anyString());
    }
  }
}
