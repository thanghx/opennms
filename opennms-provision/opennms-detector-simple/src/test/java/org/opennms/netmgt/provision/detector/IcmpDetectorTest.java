/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.provision.detector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.icmp.PingerFactory;
import org.opennms.netmgt.icmp.jna.JnaPinger;
import org.opennms.netmgt.icmp.jni.JniPinger;
import org.opennms.netmgt.provision.detector.icmp.IcmpDetector;
import org.springframework.test.annotation.IfProfileValue;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({}) 
public class IcmpDetectorTest {
    
    private IcmpDetector m_icmpDetector;
    
    
    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging();
    }
    
    @After
    public void tearDown() {
        
    }

    @Test(timeout=20000)
    @IfProfileValue(name="runPingTests", value="true")
    public void testDetectorSuccessJni() throws Exception {
        PingerFactory.setInstance(new JniPinger());
        m_icmpDetector = new IcmpDetector();
        assertTrue("ICMP could not be detected on localhost", m_icmpDetector.isServiceDetected(InetAddress.getLocalHost()));
    }

    @Test(timeout=20000)
    @IfProfileValue(name="runPingTests", value="true")
    public void testDetectorFailJni() throws Exception {
        PingerFactory.setInstance(new JniPinger());
        m_icmpDetector = new IcmpDetector();
        assertFalse("ICMP was incorrectly identified on " + InetAddressUtils.UNPINGABLE_ADDRESS.getHostAddress(), m_icmpDetector.isServiceDetected(InetAddressUtils.UNPINGABLE_ADDRESS));
    }

    @Test(timeout=20000)
    @IfProfileValue(name="runPingTests", value="true")
    public void testDetectorSuccess() throws Exception {
        PingerFactory.setInstance(new JnaPinger());
        m_icmpDetector = new IcmpDetector();
        assertTrue("ICMP could not be detected on localhost", m_icmpDetector.isServiceDetected(InetAddress.getLocalHost()));
    }

    @Test(timeout=20000)
    @IfProfileValue(name="runPingTests", value="true")
    public void testDetectorFail() throws Exception {
        PingerFactory.setInstance(new JnaPinger());
        m_icmpDetector = new IcmpDetector();
        assertFalse("ICMP was incorrectly identified on " + InetAddressUtils.UNPINGABLE_ADDRESS.getHostAddress(), m_icmpDetector.isServiceDetected(InetAddressUtils.UNPINGABLE_ADDRESS));
    }
}
