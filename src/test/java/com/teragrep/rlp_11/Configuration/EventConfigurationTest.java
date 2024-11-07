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
package com.teragrep.rlp_11.Configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class EventConfigurationTest {

    // event.hostname
    @Test
    public void testNonNullHostname() {
        Map<String, String> map = baseConfig();
        EventConfiguration eventConfiguration = EventConfigurationBuilder.build(map);
        Assertions.assertEquals("my-hostname", eventConfiguration.hostname());
    }

    @Test
    public void testNullHostname() {
        Map<String, String> map = baseConfig();
        map.remove("event.hostname");
        EventConfiguration eventConfiguration = EventConfigurationBuilder.build(map);
        Assertions.assertThrowsExactly(ConfigurationException.class, eventConfiguration::hostname);
    }

    // event.appname
    @Test
    public void testNonNullAppname() {
        Map<String, String> map = baseConfig();
        EventConfiguration eventConfiguration = EventConfigurationBuilder.build(map);
        Assertions.assertEquals("my-appname", eventConfiguration.appname());
    }

    @Test
    public void testNullAppname() {
        Map<String, String> map = baseConfig();
        map.remove("event.appname");
        EventConfiguration eventConfiguration = EventConfigurationBuilder.build(map);
        Assertions.assertThrowsExactly(ConfigurationException.class, eventConfiguration::appname);
    }

    // event.delay
    @Test
    public void testGoodDelay() {
        Map<String, String> map = baseConfig();
        EventConfiguration eventConfiguration = EventConfigurationBuilder.build(map);
        Assertions.assertEquals(15000, eventConfiguration.delay());
    }

    @Test
    public void testNullDelay() {
        Map<String, String> map = baseConfig();
        map.remove("event.delay");
        Assertions.assertThrowsExactly(ConfigurationException.class, () -> EventConfigurationBuilder.build(map));
    }

    @Test
    public void testTooSmallDelay() {
        Map<String, String> map = baseConfig();
        map.put("event.delay", "0");
        EventConfiguration eventConfiguration = EventConfigurationBuilder.build(map);
        Assertions.assertThrowsExactly(ConfigurationException.class, eventConfiguration::delay);
    }

    @Test
    public void testNonNumericDelay() {
        Map<String, String> map = baseConfig();
        map.put("event.delay", "not a number");
        Assertions.assertThrowsExactly(ConfigurationException.class, () -> EventConfigurationBuilder.build(map));
    }

    private Map<String, String> baseConfig() {
        Map<String, String> map = new HashMap<>();
        map.put("event.hostname", "my-hostname");
        map.put("event.appname", "my-appname");
        map.put("event.delay", "15000");
        return map;
    }
}
