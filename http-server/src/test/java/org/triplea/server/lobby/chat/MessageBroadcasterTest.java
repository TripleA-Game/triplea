package org.triplea.server.lobby.chat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import java.util.function.BiConsumer;
import javax.websocket.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;

@ExtendWith(MockitoExtension.class)
class MessageBroadcasterTest {

  @Mock private Session session0;
  @Mock private Session session1;
  @Mock private Session session2;
  @Mock private ServerEventEnvelope serverEventEnvelope;

  @Mock private BiConsumer<Session, ServerEventEnvelope> singleMessageSender;
  @InjectMocks private MessageBroadcaster messageBroadcaster;

  @Test
  void accept() {
    when(session0.getOpenSessions()).thenReturn(Sets.newHashSet(session0, session1, session2));
    when(session0.isOpen()).thenReturn(true);
    when(session1.isOpen()).thenReturn(true);
    when(session2.isOpen()).thenReturn(false);

    messageBroadcaster.accept(session0, serverEventEnvelope);

    verify(singleMessageSender).accept(session0, serverEventEnvelope);
    verify(singleMessageSender).accept(session1, serverEventEnvelope);
    // session2 is not open, should not be used
    verify(singleMessageSender, never()).accept(session2, serverEventEnvelope);
  }
}
