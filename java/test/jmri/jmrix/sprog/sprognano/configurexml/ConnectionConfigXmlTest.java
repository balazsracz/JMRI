package jmri.jmrix.sprog.sprognano.configurexml;

import jmri.util.JUnitUtil;
import org.junit.*;
import jmri.jmrix.sprog.sprognano.ConnectionConfig;

/**
 * ConnectionConfigXmlTest.java
 *
 * Description: tests for the ConnectionConfigXml class
 *
 * @author   Paul Bender  Copyright (C) 2016
 */
public class ConnectionConfigXmlTest extends jmri.jmrix.configurexml.AbstractSerialConnectionConfigXmlTestBase {

    // The minimal setup for log4J
    @Before
    public void setUp() {
        System.err.println("==== Start setup");
        JUnitUtil.setUp();
        xmlAdapter = new ConnectionConfigXml();
        cc = new ConnectionConfig();
    }

    @After
    public void tearDown() {
        new org.netbeans.jemmy.QueueTool().waitEmpty(20);
        JUnitUtil.tearDown();
        xmlAdapter = null;
        cc = null;
        System.err.println("==== Teardown done");
    }
}
