package games.strategy.debug;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Allows data written to a byte output stream to be read safely from a separate thread.
 * Only readFully() is currently thread safe for reading.
 */
final class SynchedByteArrayOutputStream extends ByteArrayOutputStream {
  private final Object lock = new Object();
  private final PrintStream mirror;

  SynchedByteArrayOutputStream(final PrintStream mirror) {
    this.mirror = mirror;
  }

  public void write(final byte b) {
    synchronized (lock) {
      mirror.write(b);
      super.write(b);
      lock.notifyAll();
    }
  }

  @Override
  public void write(final byte[] b, final int off, final int len) {
    synchronized (lock) {
      super.write(b, off, len);
      mirror.write(b, off, len);
      lock.notifyAll();
    }
  }

  /**
   * Read all data written to the stream.
   * Blocks until data is available.
   * This is currently the only thread safe method for reading.
   */
  String readFully() {
    synchronized (lock) {
      if (super.size() == 0) {
        try {
          lock.wait();
        } catch (final InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
      final String s = toString();
      reset();
      return s;
    }
  }
}
