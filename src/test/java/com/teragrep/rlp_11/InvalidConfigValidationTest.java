/*
 * RELP Commit Latency Probe RLP-11
 * Copyright (C) 2024 Suomen Kanuuna Oy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 *
 * Additional permission under GNU Affero General Public License version 3
 * section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with other code, such other code is not for that reason alone subject to any
 * of the requirements of the GNU Affero GPL version 3 as long as this Program
 * is the same Program as licensed from Suomen Kanuuna Oy without any additional
 * modifications.
 *
 * Supplemented terms under GNU Affero General Public License version 3
 * section 7
 *
 * Origin of the software must be attributed to Suomen Kanuuna Oy. Any modified
 * versions must be marked as "Modified version of" The Program.
 *
 * Names of the licensors and authors may not be used for publicity purposes.
 *
 * No rights are granted for use of trade names, trademarks, or service marks
 * which are in The Program if any.
 *
 * Licensee must indemnify licensors and authors for any liability that these
 * contractual assumptions impose on licensors and authors.
 *
 * To the extent this program is licensed as part of the Commercial versions of
 * Teragrep, the applicable Commercial License may apply to this file if you as
 * a licensee so wish it.
 */
package com.teragrep.rlp_11;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class InvalidConfigValidationTest {

    // event.hostname tests
    @Test
    public void TestMissingEventHostname() {
        Map<String, String> map = getDefaultMap();
        map.remove("event.hostname");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions.assertEquals("Missing <event.hostname> property", e.getMessage());
    }

    // event.appname tests
    @Test
    public void testMissingAppname() {
        Map<String, String> map = getDefaultMap();
        map.remove("event.appname");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions.assertEquals("Missing <event.appname> property", e.getMessage());
    }

    // event.delay tests
    @Test
    public void testMissingEventDelay() {
        Map<String, String> map = getDefaultMap();
        map.remove("event.delay");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions.assertEquals("Missing <event.delay> property", e.getMessage());
    }

    @Test
    public void testZeroEventDelay() {
        Map<String, String> map = getDefaultMap();
        map.put("event.delay", "0");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions.assertEquals("Invalid <event.delay> property, expected > 0, received <[0]>", e.getMessage());
    }

    @Test
    public void testNonNumericEventDelay() {
        Map<String, String> map = getDefaultMap();
        map.put("event.delay", "not a number");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions
                .assertEquals(
                        "Invalid <event.delay> property received, not a number: <For input string: \"not a number\">",
                        e.getMessage()
                );
    }

    // target.hostname tests
    @Test
    public void testMissingTargetHostname() {
        Map<String, String> map = getDefaultMap();
        map.remove("target.hostname");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions.assertEquals("Missing <target.hostname> property", e.getMessage());
    }

    // target.port tests
    @Test
    public void testMissingPort() {
        Map<String, String> map = getDefaultMap();
        map.remove("target.port");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions.assertEquals("Missing <target.port> property", e.getMessage());
    }

    @Test
    public void testTooSmallTargetPort() {
        Map<String, String> map = getDefaultMap();
        map.put("target.port", "0");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions
                .assertEquals(
                        "Invalid <target.port> property, expected between 1 and 65535, received <[0]>", e.getMessage()
                );
    }

    @Test
    public void testTooHighTargetPort() {
        Map<String, String> map = getDefaultMap();
        map.put("target.port", "65536");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions
                .assertEquals(
                        "Invalid <target.port> property, expected between 1 and 65535, received <[65536]>",
                        e.getMessage()
                );
    }

    @Test
    public void testNonNumericTargetPort() {
        Map<String, String> map = getDefaultMap();
        map.put("target.port", "not a number");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions
                .assertEquals(
                        "Invalid <target.port> property received, not a number: <For input string: \"not a number\">",
                        e.getMessage()
                );
    }

    // relp.reconnectinterval tests
    @Test
    public void testMissingReconnectInterval() {
        Map<String, String> map = getDefaultMap();
        map.remove("relp.reconnectinterval");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions.assertEquals("Missing <relp.reconnectinterval> property", e.getMessage());
    }

    @Test
    public void testZeroReconnectInterval() {
        Map<String, String> map = getDefaultMap();
        map.put("relp.reconnectinterval", "0");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions
                .assertEquals("Invalid <relp.reconnectinterval> property, expected > 0, received <[0]>", e.getMessage());
    }

    @Test
    public void testNonNumericReconnectInterval() {
        Map<String, String> map = getDefaultMap();
        map.put("relp.reconnectinterval", "not a number");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions
                .assertEquals(
                        "Invalid <relp.reconnectinterval> property received, not a number: <For input string: \"not a number\">",
                        e.getMessage()
                );
    }

    // prometheus.endport tests
    @Test
    public void testMissingPrometheusEndpoint() {
        Map<String, String> map = getDefaultMap();
        map.remove("prometheus.endpoint");
        RelpProbeConfiguration configuration = new RelpProbeConfiguration(map);
        Exception e = Assertions.assertThrowsExactly(RelpProbeConfigurationError.class, configuration::validate);
        Assertions.assertEquals("Missing <prometheus.endpoint> property", e.getMessage());
    }

    private Map<String, String> getDefaultMap() {
        Map<String, String> map = new HashMap<>();
        map.put("event.hostname", "rlp_11");
        map.put("event.appname", "rlp_11");
        map.put("event.delay", "1000");
        map.put("target.hostname", "127.0.0.1");
        map.put("target.port", "12345");
        map.put("prometheus.endpoint", "127.0.0.1:8080");
        return map;
    }
}
