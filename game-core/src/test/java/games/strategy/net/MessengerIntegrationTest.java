package games.strategy.net;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.concurrent.GuardedBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.Interruptibles;
import org.triplea.test.common.Integration;

@Integration
public class MessengerIntegrationTest {
  private int serverPort = -1;
  private IServerMessenger serverMessenger;
  private IMessenger client1Messenger;
  private IMessenger client2Messenger;
  private final MessageListener serverMessageListener = new MessageListener();
  private final MessageListener client1MessageListener = new MessageListener();
  private final MessageListener client2MessageListener = new MessageListener();

  @BeforeEach
  public void setUp() throws Exception {
    serverMessenger = new TestServerMessenger("Server", 0);
    serverMessenger.setAcceptNewConnections(true);
    serverMessenger.addMessageListener(serverMessageListener);
    serverPort = serverMessenger.getLocalNode().getSocketAddress().getPort();
    final String mac = MacFinder.getHashedMacAddress();
    client1Messenger = new ClientMessenger("localhost", serverPort, "client1", mac);
    client1Messenger.addMessageListener(client1MessageListener);
    client2Messenger = new ClientMessenger("localhost", serverPort, "client2", mac);
    client2Messenger.addMessageListener(client2MessageListener);
    assertEquals(client1Messenger.getServerNode(), serverMessenger.getLocalNode());
    assertEquals(client2Messenger.getServerNode(), serverMessenger.getLocalNode());
    assertEquals(serverMessenger.getServerNode(), serverMessenger.getLocalNode());
    await().until(serverMessenger::getNodes, hasSize(3));
  }

  @AfterEach
  public void tearDown() {
    MessengerTestUtils.shutDownQuietly(serverMessenger);
    MessengerTestUtils.shutDownQuietly(client1Messenger);
    MessengerTestUtils.shutDownQuietly(client2Messenger);
  }

  @Test
  public void testServerSend() {
    final String message = "Hello";
    serverMessenger.send(message, client1Messenger.getLocalNode());
    assertEquals(message, client1MessageListener.getLastMessage());
    assertEquals(client1MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(0, client2MessageListener.getMessageCount());
  }

  @Test
  public void testServerSendToClient2() {
    final String message = "Hello";
    serverMessenger.send(message, client2Messenger.getLocalNode());
    assertEquals(message, client2MessageListener.getLastMessage());
    assertEquals(client2MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
  }

  @Test
  public void testClientSendToServer() {
    final String message = "Hello";
    client1Messenger.send(message, serverMessenger.getLocalNode());
    assertEquals(message, serverMessageListener.getLastMessage());
    assertEquals(serverMessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
    assertEquals(0, client2MessageListener.getMessageCount());
  }

  @Test
  public void testClientSendToClient() {
    final String message = "Hello";
    client1Messenger.send(message, client2Messenger.getLocalNode());
    assertEquals(message, client2MessageListener.getLastMessage());
    assertEquals(client2MessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
    assertEquals(0, serverMessageListener.getMessageCount());
  }

  @Test
  public void testClientSendToClientLargeMessage() {
    final int count = 1_000_000;
    final StringBuilder builder = new StringBuilder(count);
    for (int i = 0; i < count; i++) {
      builder.append('a');
    }
    final String message = builder.toString();
    client1Messenger.send(message, client2Messenger.getLocalNode());
    assertEquals(client2MessageListener.getLastMessage(), message);
    assertEquals(client2MessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
    assertEquals(0, serverMessageListener.getMessageCount());
  }

  @Test
  public void testServerBroadcast() {
    final String message = "Hello";
    serverMessenger.broadcast(message);
    assertEquals(message, client1MessageListener.getLastMessage());
    assertEquals(client1MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(message, client2MessageListener.getLastMessage());
    assertEquals(client2MessageListener.getLastSender(), serverMessenger.getLocalNode());
    assertEquals(0, serverMessageListener.getMessageCount());
  }

  @Test
  public void testClientBroadcast() {
    final String message = "Hello";
    client1Messenger.broadcast(message);
    assertEquals(message, client2MessageListener.getLastMessage());
    assertEquals(client2MessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(message, serverMessageListener.getLastMessage());
    assertEquals(serverMessageListener.getLastSender(), client1Messenger.getLocalNode());
    assertEquals(0, client1MessageListener.getMessageCount());
  }

  @Test
  public void testMultipleServer() {
    for (int i = 0; i < 100; i++) {
      serverMessenger.send(i, client1Messenger.getLocalNode());
    }
    for (int i = 0; i < 100; i++) {
      client1MessageListener.clearLastMessage();
    }
  }

  @Test
  public void testMultipleClientToClient() {
    for (int i = 0; i < 100; i++) {
      client1Messenger.send(i, client2Messenger.getLocalNode());
    }
    for (int i = 0; i < 100; i++) {
      client2MessageListener.clearLastMessage();
    }
  }

  @Test
  public void testMultipleMessages() throws Exception {
    final CountDownLatch workersReadyLatch = new CountDownLatch(3);
    final Thread t1 = new Thread(new MultipleMessageSender(serverMessenger, workersReadyLatch));
    final Thread t2 = new Thread(new MultipleMessageSender(client1Messenger, workersReadyLatch));
    final Thread t3 = new Thread(new MultipleMessageSender(client2Messenger, workersReadyLatch));
    t1.start();
    t2.start();
    t3.start();
    t1.join();
    t2.join();
    t3.join();
    for (int i = 0; i < 200; i++) {
      client1MessageListener.clearLastMessage();
    }
    for (int i = 0; i < 200; i++) {
      client2MessageListener.clearLastMessage();
    }
    for (int i = 0; i < 200; i++) {
      serverMessageListener.clearLastMessage();
    }
  }

  @Test
  public void testCorrectNodeCountInRemove() {
    // when we receive the notification that a connection has been lost, the node list should reflect that change
    await().until(serverMessenger::getNodes, hasSize(3));
    final AtomicInteger serverCount = new AtomicInteger(3);
    serverMessenger.addConnectionChangeListener(new IConnectionChangeListener() {
      @Override
      public void connectionRemoved(final INode to) {
        serverCount.decrementAndGet();
      }

      @Override
      public void connectionAdded(final INode to) {
        fail("A connection should not be added.");
      }
    });
    client1Messenger.shutDown();
    await().until(serverMessenger::getNodes, hasSize(2));
    assertEquals(2, serverCount.get());
  }

  @Test
  public void testDisconnect() {
    await().until(serverMessenger::getNodes, hasSize(3));
    client1Messenger.shutDown();
    client2Messenger.shutDown();
    await().until(serverMessenger::getNodes, hasSize(1));
  }

  @Test
  public void testClose() {
    final AtomicBoolean closed = new AtomicBoolean(false);
    client1Messenger.addErrorListener(reason -> closed.set(true));
    serverMessenger.removeConnection(client1Messenger.getLocalNode());
    await().untilTrue(closed);
  }

  @Test
  public void testManyClients() throws Exception {
    final int count = 5;
    final List<ClientMessenger> clients = new ArrayList<>();
    final List<MessageListener> listeners = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      final String name = "newClient" + i;
      final String mac = MacFinder.getHashedMacAddress();
      final ClientMessenger messenger = new ClientMessenger("localhost", serverPort, name, mac);
      final MessageListener listener = new MessageListener();
      messenger.addMessageListener(listener);
      clients.add(messenger);
      listeners.add(listener);
    }

    serverMessenger.broadcast("TEST");
    for (final MessageListener listener : listeners) {
      assertEquals("TEST", listener.getLastMessage());
    }
    for (int i = 0; i < count; i++) {
      clients.get(i).shutDown();
    }
  }

  private static class MessageListener implements IMessageListener {
    private final List<Serializable> messages = new ArrayList<>();
    private final List<INode> senders = new ArrayList<>();
    private final Object lock = new Object();

    @Override
    public void messageReceived(final Serializable msg, final INode from) {
      synchronized (lock) {
        messages.add(msg);
        senders.add(from);
        lock.notifyAll();
      }
    }

    public void clearLastMessage() {
      synchronized (lock) {
        waitForMessage();
        messages.remove(0);
        senders.remove(0);
      }
    }

    public Object getLastMessage() {
      synchronized (lock) {
        waitForMessage();
        assertFalse(messages.isEmpty());
        return messages.get(0);
      }
    }

    public INode getLastSender() {
      synchronized (lock) {
        waitForMessage();
        return senders.get(0);
      }
    }

    @GuardedBy("lock")
    private void waitForMessage() {
      assert Thread.holdsLock(lock);

      while (messages.isEmpty()) {
        try {
          lock.wait(1500);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          fail("unexpected exception: " + e.getMessage());
        }
      }
    }

    public int getMessageCount() {
      synchronized (lock) {
        return messages.size();
      }
    }
  }

  private static final class MultipleMessageSender implements Runnable {
    private final IMessenger messenger;
    private final CountDownLatch workersReadyLatch;

    MultipleMessageSender(final IMessenger messenger, final CountDownLatch workersReadyLatch) {
      this.messenger = messenger;
      this.workersReadyLatch = workersReadyLatch;
    }

    @Override
    public void run() {
      workersReadyLatch.countDown();
      Interruptibles.await(workersReadyLatch);
      for (int i = 0; i < 100; i++) {
        messenger.broadcast(i);
      }
    }
  }
}
