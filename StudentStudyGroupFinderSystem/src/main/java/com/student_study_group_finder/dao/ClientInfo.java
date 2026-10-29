package com.student_study_group_finder.dao;

/**
 * Resolves the local host address recorded with each activity log entry.
 * Falls back to loopback when the host address cannot be determined.
 */
public final class ClientInfo {

    private ClientInfo() {
    }

    public static String localIpAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
