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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class RelpProbeConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelpProbeConfiguration.class);
    private final Map<String, String> config;

    public RelpProbeConfiguration(Map<String, String> config) {
        this.config = config;
    }

    public void validate() throws RelpProbeConfigurationError {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Validating the following configuration: {}", config);
        }

        if (config.get("event.hostname") == null) {
            LOGGER.error("Missing <event.hostname> property");
            throw new RelpProbeConfigurationError("Missing <event.hostname> property");
        }

        if (config.get("event.appname") == null) {
            LOGGER.error("Missing <event.appname> property");
            throw new RelpProbeConfigurationError("Missing <event.appname> property");
        }

        String eventDelay = config.get("event.delay");
        if (eventDelay == null) {
            LOGGER.error("Missing <event.delay> property");
            throw new RelpProbeConfigurationError("Missing <event.delay> property");
        }
        try {
            int eventDelayInt = Integer.parseInt(eventDelay);
            if (eventDelayInt <= 0) {
                LOGGER.error("Invalid <event.delay> property, expected > 0, received <[{}]>", eventDelay);
                throw new RelpProbeConfigurationError(
                        "Invalid <event.delay> property, expected > 0, received <[" + eventDelay + "]>"
                );
            }
        }
        catch (NumberFormatException e) {
            LOGGER.error("Invalid <event.delay> property received, not a number: <{}>", e.getMessage());
            throw new RelpProbeConfigurationError(
                    "Invalid <event.delay> property received, not a number: <" + e.getMessage() + ">"
            );
        }

        if (config.get("target.hostname") == null) {
            LOGGER.error("Missing <target.hostname> property");
            throw new RelpProbeConfigurationError("Missing <target.hostname> property");
        }

        String targetPort = config.get("target.port");
        if (targetPort == null) {
            LOGGER.error("Missing <target.port> property");
            throw new RelpProbeConfigurationError("Missing <target.port> property");
        }
        try {
            int targetPortInt = Integer.parseInt(targetPort);
            if (targetPortInt <= 0 || targetPortInt > 65535) {
                LOGGER
                        .error(
                                "Invalid <target.port> property, expected between 1 and 65535, received <[{}]>",
                                targetPort
                        );
                throw new RelpProbeConfigurationError(
                        "Invalid <target.port> property, expected between 1 and 65535, received <[" + targetPort + "]>"
                );
            }
        }
        catch (NumberFormatException e) {
            LOGGER.error("Invalid <target.port> property received, not a number: <{}>", e.getMessage());
            throw new RelpProbeConfigurationError(
                    "Invalid <target.port> property received, not a number: <" + e.getMessage() + ">"
            );
        }

        if (config.get("prometheus.endpoint") == null) {
            LOGGER.error("Missing <prometheus.endpoint> property");
            throw new RelpProbeConfigurationError("Missing <prometheus.endpoint> property");
        }

        String reconnectInterval = config.get("relp.reconnectinterval");
        if (reconnectInterval == null) {
            LOGGER.error("Missing <relp.reconnectinterval> property");
            throw new RelpProbeConfigurationError("Missing <relp.reconnectinterval> property");
        }
        try {
            int reconnectIntervalInt = Integer.parseInt(reconnectInterval);
            if (reconnectIntervalInt <= 0) {
                LOGGER
                        .error(
                                "Invalid <relp.reconnectinterval> property, property, expected > 0, received <[{}]>",
                                reconnectIntervalInt
                        );
                throw new RelpProbeConfigurationError(
                        "Invalid <relp.reconnectinterval> property, expected > 0, received <[" + reconnectInterval
                                + "]>"
                );
            }
        }
        catch (NumberFormatException e) {
            LOGGER.error("Invalid <relp.reconnectinterval> property received, not a number: <{}>", e.getMessage());
            throw new RelpProbeConfigurationError(
                    "Invalid <relp.reconnectinterval> property received, not a number: <" + e.getMessage() + ">"
            );
        }
    }

    public String getEventHostname() {
        return config.get("event.hostname");
    }

    public String getEventAppname() {
        return config.get("event.appname");
    }

    public int getEventDelay() {
        return Integer.parseInt(config.get("event.delay"));
    }

    public String getTargetHostname() {
        return config.get("target.hostname");
    }

    public int getTargetPort() {
        return Integer.parseInt(config.get("target.port"));
    }

    public String getPrometheusEndpoint() {
        return config.get("prometheus.endpoint");
    }

    public int getReconnectInterval() {
        return Integer.parseInt(config.get("relp.reconnectinterval"));
    }
}
