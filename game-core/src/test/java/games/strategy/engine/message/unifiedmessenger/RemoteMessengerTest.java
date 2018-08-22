package games.strategy.engine.message.unifiedmessenger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.message.ConnectionLostException;
import games.strategy.engine.message.IRemote;
import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteMessenger;
import games.strategy.engine.message.RemoteName;
import games.strategy.engine.message.RemoteNotFoundException;
import games.strategy.engine.message.UnifiedMessengerHub;
import games.strategy.net.ClientMessenger;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.MacFinder;
import games.strategy.net.Node;
import games.strategy.net.TestServerMessenger;
import games.strategy.util.Interruptibles;

public class RemoteMessengerTest {
  private IServerMessenger serverMessenger = mock(IServerMessenger.class);
  private RemoteMessenger remoteMessenger;
  private UnifiedMessengerHub unifiedMessengerHub;

  @BeforeEach
  public void setUp() throws Exception {
    // simple set up for non networked testing
    final List<IConnectionChangeListener> connectionListeners = new CopyOnWriteArrayList<>();
    doAnswer(invocation -> connectionListeners.add(invocation.getArgument(0)))
        .when(serverMessenger).addConnectionChangeListener(any());
    doAnswer(invocation -> {
      for (final IConnectionChangeListener listener : connectionListeners) {
        listener.connectionRemoved(invocation.getArgument(0));
      }
      return null;
    }).when(serverMessenger).removeConnection(any());
    final Node dummyNode = new Node("dummy", InetAddress.getLocalHost(), 0);
    when(serverMessenger.getLocalNode()).thenReturn(dummyNode);
    when(serverMessenger.getServerNode()).thenReturn(dummyNode);
    when(serverMessenger.isServer()).thenReturn(true);
    remoteMessenger = new RemoteMessenger(new UnifiedMessenger(serverMessenger));
  }

  @AfterEach
  public void tearDown() {
    serverMessenger = null;
    remoteMessenger = null;
  }

  @Test
  public void testRegisterUnregister() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    remoteMessenger.registerRemote(testRemote, test);
    assertTrue(remoteMessenger.hasLocalImplementor(test));
    remoteMessenger.unregisterRemote(test);
    assertFalse(remoteMessenger.hasLocalImplementor(test));
  }

  @Test
  public void testMethodCall() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    remoteMessenger.registerRemote(testRemote, test);
    final ITestRemote remote = (ITestRemote) remoteMessenger.getRemote(test);
    assertEquals(2, remote.increment(1));
    assertEquals(testRemote.getLastSenderNode(), serverMessenger.getLocalNode());
  }

  @Test
  public void testExceptionThrownWhenUnregisteredRemote() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    remoteMessenger.registerRemote(testRemote, test);
    final ITestRemote remote = (ITestRemote) remoteMessenger.getRemote(test);
    remoteMessenger.unregisterRemote("test");
    final Exception e = assertThrows(RuntimeException.class, () -> remote.increment(1));
    assertTrue(RemoteNotFoundException.class.isInstance(e.getCause()));
  }

  @Test
  public void testNoRemote() {
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    remoteMessenger.getRemote(test);
    final ITestRemote remote = (ITestRemote) remoteMessenger.getRemote(test);
    final Exception e = assertThrows(RuntimeException.class, remote::testVoid);
    assertTrue(RemoteNotFoundException.class.isInstance(e.getCause()));
  }

  @Test
  public void testVoidMethodCall() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    remoteMessenger.registerRemote(testRemote, test);
    final ITestRemote remote = (ITestRemote) remoteMessenger.getRemote(test);
    remote.testVoid();
  }

  @Test
  public void testException() {
    final TestRemote testRemote = new TestRemote();
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    remoteMessenger.registerRemote(testRemote, test);
    final ITestRemote remote = (ITestRemote) remoteMessenger.getRemote(test);
    final Exception e = assertThrows(Exception.class, remote::throwException);
    assertEquals(TestRemote.EXCEPTION_STRING, e.getCause().getMessage());
  }

  @Test
  public void testRemoteCall() throws Exception {
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    IServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new TestServerMessenger("server", 0);
      server.setAcceptNewConnections(true);
      final int serverPort = server.getLocalNode().getSocketAddress().getPort();
      final String mac = MacFinder.getHashedMacAddress();
      client = new ClientMessenger("localhost", serverPort, "client", mac);
      final UnifiedMessenger serverUnifiedMessenger = new UnifiedMessenger(server);
      unifiedMessengerHub = serverUnifiedMessenger.getHub();
      final RemoteMessenger serverRemoteMessenger = new RemoteMessenger(serverUnifiedMessenger);
      final RemoteMessenger clientRemoteMessenger = new RemoteMessenger(new UnifiedMessenger(client));
      // register it on the server
      final TestRemote testRemote = new TestRemote();
      serverRemoteMessenger.registerRemote(testRemote, test);
      // since the registration must go over a socket
      // and through a couple threads, wait for the
      // client to get it
      int waitCount = 0;
      while (!unifiedMessengerHub.hasImplementors(test.getName()) && waitCount < 20) {
        waitCount++;
        Interruptibles.sleep(50);
      }
      // call it on the client
      final int incrementedValue = ((ITestRemote) clientRemoteMessenger.getRemote(test)).increment(1);
      assertEquals(2, incrementedValue);
      assertEquals(testRemote.getLastSenderNode(), client.getLocalNode());
    } finally {
      shutdownServerAndClient(server, client);
    }
  }

  private static void shutdownServerAndClient(final IServerMessenger server, final ClientMessenger client) {
    if (server != null) {
      server.shutDown();
    }
    if (client != null) {
      client.shutDown();
    }
  }

  @Test
  public void testRemoteCall2() throws Exception {
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    IServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new TestServerMessenger("server", 0);
      server.setAcceptNewConnections(true);
      final int serverPort = server.getLocalNode().getSocketAddress().getPort();
      final String mac = MacFinder.getHashedMacAddress();
      client = new ClientMessenger("localhost", serverPort, "client", mac);
      final RemoteMessenger serverRemoteMessenger = new RemoteMessenger(new UnifiedMessenger(server));
      final TestRemote testRemote = new TestRemote();
      serverRemoteMessenger.registerRemote(testRemote, test);
      final RemoteMessenger clientRemoteMessenger = new RemoteMessenger(new UnifiedMessenger(client));
      // call it on the client
      // should be no need to wait since the constructor should not
      // reutrn until the initial state of the messenger is good
      final int incrementedValue = ((ITestRemote) clientRemoteMessenger.getRemote(test)).increment(1);
      assertEquals(2, incrementedValue);
      assertEquals(testRemote.getLastSenderNode(), client.getLocalNode());
    } finally {
      shutdownServerAndClient(server, client);
    }
  }

  @Test
  public void testShutDownClient() throws Exception {
    // when the client shutdown, remotes created
    // on the client should not be visible on server
    final RemoteName test = new RemoteName("test", ITestRemote.class);
    IServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new TestServerMessenger("server", 0);
      server.setAcceptNewConnections(true);
      final int serverPort = server.getLocalNode().getSocketAddress().getPort();
      final String mac = MacFinder.getHashedMacAddress();
      client = new ClientMessenger("localhost", serverPort, "client", mac);
      final UnifiedMessenger serverUnifiedMessenger = new UnifiedMessenger(server);
      final RemoteMessenger clientRemoteMessenger = new RemoteMessenger(new UnifiedMessenger(client));
      clientRemoteMessenger.registerRemote(new TestRemote(), test);
      serverUnifiedMessenger.getHub().waitForNodesToImplement(test.getName());
      assertTrue(serverUnifiedMessenger.getHub().hasImplementors(test.getName()));
      client.shutDown();
      Interruptibles.sleep(200);
      assertTrue(!serverUnifiedMessenger.getHub().hasImplementors(test.getName()));
    } finally {
      shutdownServerAndClient(server, client);
    }
  }

  @Test
  public void testMethodReturnsOnWait() throws Exception {
    // when the client shutdown, remotes created
    // on the client should not be visible on server
    final RemoteName test = new RemoteName("test", IFoo.class);
    IServerMessenger server = null;
    ClientMessenger client = null;
    try {
      server = new TestServerMessenger("server", 0);
      server.setAcceptNewConnections(true);
      final int serverPort = server.getLocalNode().getSocketAddress().getPort();
      final String mac = MacFinder.getHashedMacAddress();
      client = new ClientMessenger("localhost", serverPort, "client", mac);
      final UnifiedMessenger serverUnifiedMessenger = new UnifiedMessenger(server);
      final RemoteMessenger serverRemoteMessenger = new RemoteMessenger(serverUnifiedMessenger);
      final RemoteMessenger clientRemoteMessenger = new RemoteMessenger(new UnifiedMessenger(client));
      final CountDownLatch clientReadySignal = new CountDownLatch(1);
      final CountDownLatch testCompleteSignal = new CountDownLatch(1);
      final IFoo foo = () -> {
        clientReadySignal.countDown();
        Interruptibles.await(testCompleteSignal);
      };
      clientRemoteMessenger.registerRemote(foo, test);
      serverUnifiedMessenger.getHub().waitForNodesToImplement(test.getName());
      assertTrue(serverUnifiedMessenger.getHub().hasImplementors(test.getName()));
      final CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        final IFoo remoteFoo = (IFoo) serverRemoteMessenger.getRemote(test);
        remoteFoo.foo();
      });
      // wait for client to start
      Interruptibles.await(clientReadySignal);
      client.shutDown();
      testCompleteSignal.countDown();
      final Exception e = assertThrows(ExecutionException.class, future::get);
      assertTrue(ConnectionLostException.class.isInstance(e.getCause().getCause()));
    } finally {
      shutdownServerAndClient(server, client);
    }
  }

  private interface IFoo extends IRemote {
    void foo();
  }

  private interface ITestRemote extends IRemote {
    int increment(int testVal);

    void testVoid();

    void throwException() throws Exception;
  }

  private static class TestRemote implements ITestRemote {
    static final String EXCEPTION_STRING = "AND GO";
    private INode senderNode;

    @Override
    public int increment(final int testVal) {
      senderNode = MessageContext.getSender();
      return testVal + 1;
    }

    @Override
    public void testVoid() {
      senderNode = MessageContext.getSender();
    }

    @Override
    public void throwException() throws Exception {
      throw new Exception(EXCEPTION_STRING);
    }

    INode getLastSenderNode() {
      return senderNode;
    }
  }
}
