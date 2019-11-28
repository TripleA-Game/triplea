package org.triplea.http.client;

import com.google.common.base.Preconditions;
import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class IpAddressParser {

  /**
   * Parses a given IP address represented as a string and returns an equivalent {@code
   * InetAddress}. Handles IPv4 or Ipv6 addresses.
   *
   * @throws NullPointerException thrown if input is null.
   * @throws IllegalArgumentException thrown if input is empty or not a valid IP address.
   */
  public static InetAddress fromString(final String ipString) {
    Preconditions.checkNotNull(ipString);
    Preconditions.checkArgument(!ipString.isEmpty());
    try {
      return InetAddress.getByName(ipString);
    } catch (final UnknownHostException e) {
      throw new IllegalArgumentException("Invalid IP address: " + ipString + ", " + e.getMessage());
    }
  }
}
