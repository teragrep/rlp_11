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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TargetConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TargetConfiguration.class);
    private final Map<String, String> config;

    public TargetConfiguration(final Map<String, String> config) {
        this.config = config;
    }

    public String hostname() {
        final String hostname = config.get("target.hostname");
        if (hostname == null) {
            LOGGER.error("Configuration failure: <target.hostname> is null");
            throw new ConfigurationException("Invalid value for <target.hostname> received");
        }
        return hostname;
    }

    public int port() {
        final String portString = config.get("target.port");
        if (portString == null) {
            LOGGER.error("Configuration failure: <target.port> is null");
            throw new ConfigurationException("Invalid value for <target.port> received");
        }
        final int port;
        try {
            port = Integer.parseInt(portString);
        }
        catch (NumberFormatException e) {
            LOGGER.error("Configuration failure: Invalid value for <target.port>: <{}>", e.getMessage());
            throw e;
        }
        if (port < 1 || port > 65535) {
            LOGGER
                    .error(
                            "Configuration failure: <target.port> <[{}]> is in invalid range, expected between 1 and 65535",
                            port
                    );
            throw new ConfigurationException("Invalid value for <target.port> received");
        }
        return port;
    }

    public int reconnectInterval() {
        final String reconnectIntervalString = config.get("target.reconnectinterval");
        if (reconnectIntervalString == null) {
            LOGGER.error("Configuration failure: <target.reconnectinterval> is null");
            throw new ConfigurationException("Invalid value for <target.reconnectinterval> received");
        }
        final int reconnectInterval;
        try {
            reconnectInterval = Integer.parseInt(reconnectIntervalString);
        }
        catch (NumberFormatException e) {
            LOGGER.error("Configuration failure: Invalid value for <target.reconnectinterval>: <{}>", e.getMessage());
            throw e;
        }
        if (reconnectInterval <= 0) {
            LOGGER
                    .error(
                            "Configuration failure: <target.reconnectinterval> <[{}]> too small, expected to be >0",
                            reconnectInterval
                    );
            throw new ConfigurationException("Invalid value for <target.reconnectinterval> received");
        }
        return reconnectInterval;
    }
}
