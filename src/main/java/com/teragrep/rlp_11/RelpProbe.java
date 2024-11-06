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

import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.jmx.JmxReporter;
import com.teragrep.rlo_14.Facility;
import com.teragrep.rlo_14.Severity;
import com.teragrep.rlo_14.SyslogMessage;
import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.RelpConnection;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jakarta.json.Json;
import jakarta.json.JsonObject;

public class RelpProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelpProbe.class);
    private final RelpProbeConfiguration config;
    private boolean stayRunning = true;
    private RelpConnection relpConnection;
    private final CountDownLatch latch = new CountDownLatch(1);
    private boolean connected = false;
    public final Metrics metrics;
    private final JmxReporter jmxReporter;
    private final Slf4jReporter slf4jReporter;
    private final Server jettyServer;

    public RelpProbe(final RelpProbeConfiguration config) {
        this(config, new Metrics(config.getTargetHostname() + ":" + config.getTargetPort()));
    }

    public RelpProbe(final RelpProbeConfiguration config, final Metrics metrics) {
        this(
                config,
                metrics,
                JmxReporter.forRegistry(metrics.metricRegistry).build(),
                Slf4jReporter.forRegistry(metrics.metricRegistry).outputTo(LoggerFactory.getLogger(RelpProbe.class)).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build(), new Server(config.getPrometheusPort())
        );
    }

    public RelpProbe(
            final RelpProbeConfiguration config,
            final Metrics metrics,
            JmxReporter jmxReporter,
            Slf4jReporter slf4jReporter,
            Server jettyServer
    ) {
        this.config = config;
        this.metrics = metrics;
        this.jmxReporter = jmxReporter;
        this.slf4jReporter = slf4jReporter;
        this.jettyServer = jettyServer;
    }

    public void start() {
        this.jmxReporter.start();
        this.slf4jReporter.start(1, TimeUnit.MINUTES);
        String origin;
        try {
            origin = InetAddress.getLocalHost().getHostName();
        }
        catch (UnknownHostException e) {
            origin = "localhost";
            LOGGER.warn("Could not get hostname, using <{}> instead", origin);
        }
        relpConnection = new RelpConnection();
        connect();
        final int eventDelay = config.getEventDelay();
        while (stayRunning) {
            final RelpBatch relpBatch = new RelpBatch();
            final Instant timestamp = Instant.now();
            final JsonObject event = Json
                    .createObjectBuilder()
                    .add("origin", origin)
                    .add("timestamp", timestamp.getEpochSecond() + "." + timestamp.getNano())
                    .build();
            final byte[] record = new SyslogMessage()
                    .withTimestamp(timestamp.toEpochMilli())
                    .withAppName(config.getEventAppname())
                    .withHostname(config.getEventHostname())
                    .withFacility(Facility.USER)
                    .withSeverity(Severity.INFORMATIONAL)
                    .withMsg(event.toString())
                    .toRfc5424SyslogMessage()
                    .getBytes(StandardCharsets.UTF_8);
            relpBatch.insert(record);

            boolean allSent = false;
            while (!allSent && stayRunning) {
                try (final Timer.Context context = metrics.sendLatency.time()) {
                    LOGGER.debug("Committing Relpbatch");
                    relpConnection.commit(relpBatch);
                    metrics.records.inc();
                }
                catch (IllegalStateException | IOException | java.util.concurrent.TimeoutException e) {
                    LOGGER.warn("Failed to commit: <{}>", e.getMessage());
                    relpConnection.tearDown();
                    connected = false;
                }
                LOGGER.debug("Verifying Transaction");
                allSent = relpBatch.verifyTransactionAll();
                if (!allSent) {
                    LOGGER.warn("Transactions failed, retrying");
                    metrics.resends.inc();
                    relpBatch.retryAllFailed();
                    reconnect();
                }
            }
            try {
                LOGGER.debug("Sleeping before sending next event");
                Thread.sleep(eventDelay);
            }
            catch (InterruptedException e) {
                LOGGER.warn("Sleep interrupted: <{}>", e.getMessage());
            }
        }
        disconnect();
        latch.countDown();
        slf4jReporter.close();
        jmxReporter.close();
        try {
            jettyServer.stop();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void connect() {
        while (!connected && stayRunning) {
            try (final Timer.Context context = metrics.connectLatency.time()) {
                LOGGER.info("Connecting to <[{}:{}]>", config.getTargetHostname(), config.getTargetPort());
                connected = relpConnection.connect(config.getTargetHostname(), config.getTargetPort());
                LOGGER.info("Connected.");
                metrics.connects.inc();
            }
            catch (TimeoutException | IOException e) {
                LOGGER
                        .warn(
                                "Failed to connect to <[{}:{}]>: <{}>", config.getTargetHostname(),
                                config.getTargetPort(), e.getMessage()
                        );
            }
            if (!connected) {
                try {
                    LOGGER.info("Sleeping for <[{}]>ms before reconnecting", config.getReconnectInterval());
                    Thread.sleep(config.getReconnectInterval());
                    metrics.retriedConnects.inc();
                }
                catch (InterruptedException e) {
                    LOGGER.warn("Sleep was interrupted: <{}>", e.getMessage());
                }
            }
        }
    }

    private void reconnect() {
        disconnect();
        connect();
    }

    private void disconnect() {
        if (!connected) {
            LOGGER.debug("No need to disconnect, not connected");
            return;
        }
        try {
            LOGGER.info("Disconnecting..");
            relpConnection.disconnect();
            metrics.disconnects.inc();
        }
        catch (IOException | TimeoutException e) {
            LOGGER.warn("Failed to disconnect: <{}>", e.getMessage());
        }
        relpConnection.tearDown();
        LOGGER.info("Disconnected.");
        connected = false;
    }

    public void stop() {
        LOGGER.debug("Stop called");
        stayRunning = false;
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out while waiting for probe to shutdown.");
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("RelpProbe stopped.");
    }
}
