package games.strategy.engine.message;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.util.Tuple;

/**
 * All the info neccassary to describe a method call in one handy
 * serializable package.
 */
public class RemoteMethodCall implements Externalizable {
  private static final long serialVersionUID = 4630825927685836207L;
  private static final Logger logger = Logger.getLogger(RemoteMethodCall.class.getName());
  private String m_remoteName;
  private String m_methodName;
  private Object[] m_args;
  // to save space, we dont serialize method name/types
  // instead we just serialize a number which can be transalted into
  // the correct method.
  private int m_methodNumber;
  // stored as a String[] so we can be serialzed
  private String[] m_argTypes;

  public RemoteMethodCall() {}

  public RemoteMethodCall(final String remoteName, final String methodName, final Object[] args,
      final Class<?>[] argTypes, final Class<?> remoteInterface) {
    if (argTypes == null) {
      throw new IllegalArgumentException("ArgTypes are null");
    }
    if (args == null && argTypes.length != 0) {
      throw new IllegalArgumentException("args but no types");
    }
    if (args != null && args.length != argTypes.length) {
      throw new IllegalArgumentException("Arg and arg type lengths dont match");
    }
    m_remoteName = remoteName;
    m_methodName = methodName;
    m_args = args;
    m_argTypes = classesToString(argTypes, args);
    m_methodNumber = RemoteInterfaceHelper.getNumber(methodName, argTypes, remoteInterface);
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Remote Method Call:" + debugMethodText());
    }
  }

  private String debugMethodText() {
    if (m_argTypes == null) {
      return "." + m_methodName + "(" + ")";
    } else {
      return "." + m_methodName + "(" + Arrays.asList(m_argTypes) + ")";
    }
  }

  /**
   * @return Returns the channelName.
   */
  public String getRemoteName() {
    return m_remoteName;
  }

  /**
   * @return Returns the methodName.
   */
  public String getMethodName() {
    return m_methodName;
  }

  /**
   * @return Returns the args.
   */
  public Object[] getArgs() {
    return m_args;
  }

  /**
   * @return Returns the argTypes.
   */
  public Class<?>[] getArgTypes() {
    return stringsToClasses(m_argTypes, m_args);
  }

  private static Class<?>[] stringsToClasses(final String[] strings, final Object[] args) {
    final Class<?>[] classes = new Class<?>[strings.length];
    for (int i = 0; i < strings.length; i++) {
      try {
        // null if we skipped writing because the arg is the expected
        // class, this saves some space since generally the arg will
        // be of the correct type
        if (strings[i] == null) {
          classes[i] = args[i].getClass();
        } else if (strings[i].equals("int")) {
          classes[i] = Integer.TYPE;
        } else if (strings[i].equals("short")) {
          classes[i] = Short.TYPE;
        } else if (strings[i].equals("byte")) {
          classes[i] = Byte.TYPE;
        } else if (strings[i].equals("long")) {
          classes[i] = Long.TYPE;
        } else if (strings[i].equals("float")) {
          classes[i] = Float.TYPE;
        } else if (strings[i].equals("double")) {
          classes[i] = Double.TYPE;
        } else if (strings[i].equals("boolean")) {
          classes[i] = Boolean.TYPE;
        } else {
          classes[i] = Class.forName(strings[i]);
        }
      } catch (final ClassNotFoundException e) {
        throw new IllegalStateException(e);
      }
    }
    return classes;
  }

  private static String[] classesToString(final Class<?>[] classes, final Object[] args) {
    // as an optimization, if args[i].getClass == classes[i] then leave classes[i] as null
    // this will reduce the amount of info we write over the network in the common
    // case where the object is the same type as its arg
    final String[] string = new String[classes.length];
    for (int i = 0; i < classes.length; i++) {
      if (args != null && args[i] != null && classes[i] == args[i].getClass()) {
        string[i] = null;
      } else {
        string[i] = classes[i].getName();
      }
    }
    return string;
  }

  @Override
  public String toString() {
    return "Remote method call:" + m_methodName + " on:" + m_remoteName;
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.writeUTF(m_remoteName);
    out.writeByte(m_methodNumber);
    if (m_args == null) {
      out.writeByte(Byte.MAX_VALUE);
    } else {
      out.writeByte(m_args.length);
      for (final Object arg : m_args) {
        out.writeObject(arg);
      }
    }
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    m_remoteName = in.readUTF();
    m_methodNumber = in.readByte();
    final byte count = in.readByte();
    if (count != Byte.MAX_VALUE) {
      m_args = new Object[count];
      for (int i = 0; i < count; i++) {
        m_args[i] = in.readObject();
      }
    }
  }

  /**
   * After we have been de-serialized, we do not transmit enough
   * informatin to determine the method without being told
   * what class we operate on.
   */
  public void resolve(final Class<?> remoteType) {
    if (m_methodName != null) {
      return;
    }
    final Tuple<String, Class<?>[]> values = RemoteInterfaceHelper.getMethodInfo(m_methodNumber, remoteType);
    m_methodName = values.getFirst();
    m_argTypes = classesToString(values.getSecond(), m_args);
    if (logger.isLoggable(Level.FINE)) {
      logger.fine("Remote Method for class:" + remoteType.getSimpleName() + " Resolved To:" + debugMethodText());
    }
  }
}
