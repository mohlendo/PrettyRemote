package com.mohleno.prettyremote.services;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class LGConnectServiceTest extends TestCase {

    public void testParseSessionKey() throws Exception {
        LGConnectService service = new LGConnectService();
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><envelope><HDCPError>200</HDCPError><HDCPErrorDetail>OK</HDCPErrorDetail><session>114859659</session></envelope>";
        InputStream stream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));

        assertEquals(service.parseSessionKey(stream), "114859659");
    }
}