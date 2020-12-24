package fleetmanagement.config;

import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.*;

public class NetworkHelper {

    private static final Logger logger = Logger.getLogger(NetworkHelper.class);
    public static final String MAC_ADDRESS_DUMMY = "XX:XX:XX:XX:XX:XX";

    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    private static byte[] getAnyMacAddress() throws UnknownHostException, SocketException {
        // Iterate all NICs (network interface cards)...

        for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
            NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
            return iface.getHardwareAddress();
        }

        return null;
    }

    static String getMacAddressDummy() {
        return MAC_ADDRESS_DUMMY;
    }

    static String getMacAddress() {
        InetAddress ip = null;
        try {
            ip = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            logger.info("Can't get IP using InetAddress.getLocalHost()");
        }
        if (ip == null)
            try {
                ip = getLocalHostLANAddress();
            } catch (UnknownHostException e) {
                logger.info("Can't get IP using getLocalHostLANAddress()");
            }

        byte[] mac = null;
        NetworkInterface network = null;

        if (ip != null) {
            try {
                network = NetworkInterface.getByInetAddress(ip);
            } catch (SocketException e) {
                logger.error("Can't get network by ip address");
            }
        }

        if (network != null) {
            try {
                mac = network.getHardwareAddress();
            } catch (SocketException e) {
                logger.error("Can't get mac address from found network");
            }
        }

        if (mac == null) {
            logger.info("Trying to find any available mac address");
            try {
                mac = getAnyMacAddress();
            } catch (UnknownHostException | SocketException e) {
                logger.error("Can't find any mac address", e);
                return null;
            }
        }
        if (mac == null) {
            return null;
        }
        logger.info("network hardware address " + mac.toString());
        return javax.xml.bind.DatatypeConverter.printHexBinary(mac);

    }

    static Set<String> getAllMacAddresses() throws SocketException {
        final Enumeration<NetworkInterface> inetAddresses = NetworkInterface.getNetworkInterfaces();
        final Set<String> addresses = new TreeSet<>();

        while (inetAddresses.hasMoreElements()) {
            final byte[] macBytes = inetAddresses.nextElement().getHardwareAddress();

            if (macBytes == null)
                continue;

            addresses.add(javax.xml.bind.DatatypeConverter.printHexBinary(macBytes));
        }
        return addresses;
    }
}
