package iTests.framework.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkUtils {

    public static boolean isThisMyIpAddress(String ip) {
        InetAddress addr;
        try {
            addr = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return false;
        }

        // Check if the address is a valid special local or loop back
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress())
            return true;

        // Check if the address is defined on any interface
        try {
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (SocketException e) {
            return false;
        }
    }

    public static String[] resolveIpsToHostNames(String[] ips) {

        String[] machinesHostNames = new String[ips.length];

        for (int i = 0 ; i < ips.length ; i++) {
            String host = ips[i];
            machinesHostNames[i] = resolveIpToHostName(host);
        }
        
        return machinesHostNames;
    }

    public static String resolveIpToHostName(String ip) {

    	try {
    		InetAddress addr = InetAddress.getByName(ip);
            return addr.getHostName();
    	} catch (UnknownHostException e) {
    		throw new IllegalStateException("could not resolve host name of ip " + ip);
    	}
        
        
        /*String[] chars = ip.split("\\.");

        byte[] add = new byte[chars.length];
        for (int j = 0 ; j < chars.length ; j++) {
            add[j] =(byte) ((int) Integer.valueOf(chars[j]));
        }

        try {
            InetAddress byAddress = InetAddress.getByAddress(add);
            String hostName = byAddress.getHostName();
            LogUtils.log("IP address " + ip + " was resolved to " + hostName);
            return hostName;
        } catch (UnknownHostException e) {
            throw new IllegalStateException("could not resolve host name of ip " + ip);
        }*/

    }

    public static String resolveHostNameToIp(String hostName) throws UnknownHostException {
        InetAddress byName = InetAddress.getByName(hostName);
        return byName.getHostAddress();
    }

}
