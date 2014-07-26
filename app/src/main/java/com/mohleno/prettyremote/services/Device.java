package com.mohleno.prettyremote.services;

import java.io.Serializable;

/**
 * Created by moh on 25.07.14.
 */
public class Device implements Serializable {

    private String name;
    private String IP;
    private String session;

    public Device(String IP) {
        this(IP, null);
    }

    public Device(String IP, String name) {
        this.name = name;
        this.IP = IP;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Device device = (Device) o;

        return IP.equals(device.IP);

    }

    @Override
    public int hashCode() {
        return IP.hashCode();
    }
}
