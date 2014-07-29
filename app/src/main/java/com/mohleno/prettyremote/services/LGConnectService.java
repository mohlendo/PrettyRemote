package com.mohleno.prettyremote.services;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by moh on 25.07.14.
 */
public final class LGConnectService {

    private static final String TAG = LGConnectService.class.getSimpleName();
    private static final String UDP_HOST = "239.255.255.250";
    private static final int UDP_PORT = 1900;
    private static final String DISCOVER_XMIT = "M-SEARCH * HTTP/1.1" + "\r\n" +
            "HOST: 239.255.255.250:1900" + "\r\n" +
            "MAN: \"ssdp:discover\"" + "\r\n" +
            "MX: 2" + "\r\n" +
            "ST: urn:schemas-upnp-org:device:MediaRenderer:1" + "\r\n" + "\r\n";

    private static final String AUTH_URL = "http://%s:8080/roap/api/auth";
    private static final String COMMAND_URL = "http://%s:8080/udap/api/command";

    private final Pattern locationPattern = Pattern.compile("http://([0-9\\.]+)");
    private final Pattern deviceNamePattern = Pattern.compile("DLNADeviceName.lge.com: (.+)");

    private static LGConnectService instance;


    private LGConnectService(Context context) {
    }

    public static LGConnectService getInstance(Context context) {
        LGConnectService r = instance;
        if (r == null) {
            synchronized (LGConnectService.class) { // while we were waiting for the lock, another
                r = instance; // thread may have instantiated instance
                if (r == null) {
                    r = new LGConnectService(context);
                    instance = r;
                }
            }
        }
        return r;
    }

    public List<Device> scanForDevices() throws IOException {
        InetAddress group = InetAddress.getByName(UDP_HOST);
        DatagramSocket socket = new DatagramSocket();
        socket.setBroadcast(true);
        socket.setSoTimeout(3000);

        DatagramPacket getIpPacket = new DatagramPacket(DISCOVER_XMIT.getBytes(), DISCOVER_XMIT.length(),
                group, UDP_PORT);
        socket.send(getIpPacket);

        // get their responses!
        byte[] buf = new byte[512];
        DatagramPacket responsePacket = new DatagramPacket(buf, buf.length, group, UDP_PORT);
        socket.receive(responsePacket);
        String out = new String(responsePacket.getData());

        String result = out.substring(0, responsePacket.getLength());
        Log.i(TAG, "Device: \n" + result);
        Matcher matcher = locationPattern.matcher(result);

        String ip = null, deviceName = null;
        if (matcher.find()) {
            ip = matcher.group(1);
        }

        matcher = deviceNamePattern.matcher(result);
        if (matcher.find()) {
            deviceName = URLDecoder.decode(matcher.group(1), "UTF-8");
        }

        if (ip != null && deviceName != null) {
            return Collections.singletonList(new Device(ip, deviceName));
        }

        return Collections.emptyList();
    }

    public boolean requestPairingKey(Device device) throws IOException {
        URL uri;
        try {
            uri = new URL(String.format(AUTH_URL, device.getIP()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestProperty("Content-Type", "application/atom+xml");
        connection.setDoInput(true);
        connection.setDoOutput(true);

        connection.setRequestMethod("POST");
        String data = "<?xml version=\"1.0\" encoding=\"utf-8\"?><auth><type>AuthKeyReq</type></auth>";

        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(data);
        writer.flush();
        writer.close();
        os.close();

        connection.connect();

        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    public String pair(Device device, String pairingKey) throws IOException, XmlPullParserException {
        URL uri;
        try {
            uri = new URL(String.format(AUTH_URL, device.getIP()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestProperty("Content-Type", "application/atom+xml");
        connection.setDoInput(true);
        connection.setDoOutput(true);

        connection.setRequestMethod("POST");
        String data = "<?xml version=\"1.0\" encoding=\"utf-8\"?><auth><type>AuthReq</type><value>" + pairingKey + "</value></auth>";

        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(data);
        writer.flush();
        writer.close();
        os.close();

        connection.connect();

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return null;
        }

        return parseSessionKey(connection.getInputStream());
    }

    public boolean sendKeyInput(Device device, LGKey command) throws IOException {
        return sendCommand(device, "<?xml version=\"1.0\" encoding=\"utf-8\"?><command>"
                + "<name>HandleKeyInput</name><value>"
                + command.code()
                + "</value></command>");
    }

    public boolean sendTouchMove(Device device, Point point) throws IOException {
        return sendCommand(device, "<?xml version=\"1.0\" encoding=\"utf-8\"?><command>"
                + "<name>HandleTouchMove</name><value>"
                + "<x>" + point.x + "</x>"
                + "<y>" + point.y + "</y>"
                + "</value></command>");
    }

    public boolean sendTouchClick(Device device) throws IOException {
        return sendCommand(device, "<?xml version=\"1.0\" encoding=\"utf-8\"?><command>"
                + "<name>HandleTouchClick</name><value>"
                + "</value></command>");
    }



    private boolean sendCommand(Device device, String xml) throws IOException {
        URL uri;
        try {
            uri = new URL(String.format(COMMAND_URL, device.getIP()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestProperty("Content-Type", "application/atom+xml");
        connection.setDoInput(true);
        connection.setDoOutput(true);

        connection.setRequestMethod("POST");

        OutputStream os = connection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(os, "UTF-8"));
        writer.write(xml);
        writer.flush();
        writer.close();
        os.close();

        connection.connect();

        int responseCode = connection.getResponseCode();
        return responseCode == HttpURLConnection.HTTP_OK;
    }

    public String parseSessionKey(InputStream xmlInputStream) throws XmlPullParserException, IOException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(xmlInputStream, null);
        parser.nextTag();
        parser.require(XmlPullParser.START_TAG, null, "envelope");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("session")) {
                parser.require(XmlPullParser.START_TAG, null, "session");
                if (parser.next() == XmlPullParser.TEXT) {
                    return parser.getText();
                }
            } else {
                skip(parser);
            }
        }
        return null;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

}
