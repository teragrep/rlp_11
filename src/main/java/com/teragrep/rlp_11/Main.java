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

import com.teragrep.cnf_01.PathConfiguration;
import com.teragrep.rlp_11.Configuration.ProbeConfiguration;
import com.teragrep.rlp_11.Configuration.RecordConfiguration;
import com.teragrep.rlp_11.Configuration.MetricsConfiguration;
import com.teragrep.rlp_11.Configuration.PrometheusConfiguration;
import com.teragrep.rlp_11.Configuration.TargetConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) throws com.teragrep.cnf_01.ConfigurationException {
        final PathConfiguration pathConfiguration = new PathConfiguration(
                System.getProperty("configurationPath", "etc/rlp_11.properties")
        );
        final Map<String, String> map;
        try {
            map = pathConfiguration.asMap();
        }
        catch (com.teragrep.cnf_01.ConfigurationException e) {
            LOGGER.error("Failed to create PathConfiguration: <{}>", e.getMessage());
            throw e;
        }
        final PrometheusConfiguration prometheusConfiguration = new PrometheusConfiguration(map);
        final RecordConfiguration recordConfiguration = new RecordConfiguration(map);
        final TargetConfiguration targetConfiguration = new TargetConfiguration(map);
        final MetricsConfiguration metricsConfiguration = new MetricsConfiguration(map);
        final RecordFactory recordFactory = new RecordFactory(
                getHostname(),
                recordConfiguration.hostname(),
                recordConfiguration.appname()
        );
        final ProbeConfiguration probeConfiguration = new ProbeConfiguration(map);
        final RelpProbe relpProbe = new RelpProbe(
                targetConfiguration,
                probeConfiguration,
                prometheusConfiguration,
                metricsConfiguration,
                recordFactory
        );
        final Thread shutdownHook = new Thread(() -> {
            LOGGER.debug("Stopping RelpProbe..");
            relpProbe.stop();
            LOGGER.debug("Shutting down.");
        });
        Runtime.getRuntime().addShutdownHook(shutdownHook);
        LOGGER
                .info(
                        "Sending records to <[{}:{}]> every <[{}]> milliseconds", targetConfiguration.hostname(),
                        targetConfiguration.port(), probeConfiguration.interval()
                );
        LOGGER
                .info(
                        "Using hostname <[{}]> and appname <[{}]> for the records.", recordConfiguration.hostname(),
                        recordConfiguration.appname()
                );
        relpProbe.start();
    }

    private static String getHostname() {
        String origin;
        try {
            origin = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            origin = "localhost";
            LOGGER.warn("Could not get hostname, using <{}> instead", origin);
        }
        return origin;
    }
}
