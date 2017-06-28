package games.strategy.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Utility for getting/setting java bean style properties on an object.
 */
public class PropertyUtil {

  public static void set(final String propertyName, final Object value, final Object subject) {
    final Method m = getSetter(propertyName, subject, value);
    try {
      m.setAccessible(true);
      m.invoke(subject, value);
    } catch (final Exception e) {
      throw new IllegalStateException(
          "Could not set property:" + propertyName + " subject:" + subject + " new value:" + value, e);
    }
  }

  /**
   * You don't want to clear the variable first unless you are setting some variable where the setting method is
   * actually adding things to a
   * list rather than overwriting.
   */
  public static void set(final String propertyName, final Object value, final Object subject,
      final boolean resetFirst) {
    if (resetFirst) {
      reset(propertyName, subject);
    }
    set(propertyName, value, subject);
  }

  public static void reset(final String propertyName, final Object subject) {
    try {
      final Method c = getResetter(propertyName, subject);
      c.setAccessible(true);
      c.invoke(subject);
    } catch (final Exception e) {
      throw new IllegalStateException("Could not reset property:" + propertyName + " subject:" + subject, e);
    }
  }

  public static Field getFieldIncludingFromSuperClasses(final Class<?> c, final String name,
      final boolean justFromSuper) {
    if (!justFromSuper) {
      try {
        return c.getDeclaredField(name); // TODO: unchecked reflection
      } catch (final NoSuchFieldException e) {
        return getFieldIncludingFromSuperClasses(c, name, true);
      }
    } else {
      if (c.getSuperclass() == null) {
        throw new IllegalStateException("No such Property Field: " + name);
      }
      try {
        return c.getSuperclass().getDeclaredField(name); // TODO: unchecked reflection
      } catch (final NoSuchFieldException e) {
        return getFieldIncludingFromSuperClasses(c.getSuperclass(), name, true);
      }
    }
  }

  public static Object getPropertyFieldObject(final String propertyName, final Object subject) {
    try {
      final Field field = getPropertyField(propertyName, subject);
      field.setAccessible(true);
      return field.get(subject);
    } catch (final Exception e) {
      final String msg =
          "No such Property Field named: " + "m_" + propertyName + ", or: " + propertyName + ", for Subject: "
              + subject.toString();
      throw new IllegalStateException(msg, e);
    }
  }

  private static Field getPropertyField(final String propertyName, final Object subject) {
    try {
      return getFieldIncludingFromSuperClasses(subject.getClass(), "m_" + propertyName, false);
    } catch (final Exception e) {
      try {
        return getFieldIncludingFromSuperClasses(subject.getClass(), propertyName, false);
      } catch (final Exception exception) {
        throw exception;
      }
    }
  }

  private static String capitalizeFirstLetter(final String str) {
    char first = str.charAt(0);
    first = Character.toUpperCase(first);
    return first + str.substring(1);
  }

  private static Method getSetter(final String propertyName, final Object subject, final Object value) {
    final String setterName = "set" + capitalizeFirstLetter(propertyName);
    // for (final Method m : subject.getClass().getDeclaredMethods())
    for (final Method m : subject.getClass().getMethods()) {
      if (m.getName().equals(setterName)) {
        try {
          final Class<?> argType = value.getClass();
          return subject.getClass().getMethod(setterName, argType);
        } catch (final NoSuchMethodException nsmf) {
          // Go ahead and try the first one
          return m;
        } catch (final NullPointerException n) {
          // Go ahead and try the first one
          return m;
        }
      }
    }
    throw new IllegalStateException("No method called:" + setterName + " on:" + subject);
  }

  private static Method getResetter(final String propertyName, final Object subject) {
    final String resetterName = "reset" + capitalizeFirstLetter(propertyName);
    // for (final Method c : subject.getClass().getDeclaredMethods())
    for (final Method c : subject.getClass().getMethods()) {
      if (c.getName().equals(resetterName)) {
        try {
          return subject.getClass().getMethod(resetterName);
        } catch (final NoSuchMethodException nsmf) {
          // Go ahead and try the first one
          return c;
        } catch (final NullPointerException n) {
          // Go ahead and try the first one
          return c;
        }
      }
    }
    throw new IllegalStateException("No method called:" + resetterName + " on:" + subject);
  }
}
