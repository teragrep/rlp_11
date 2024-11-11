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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.RelpConnection;
import com.teragrep.rlp_11.Configuration.ProbeConfiguration;
import com.teragrep.rlp_11.Configuration.MetricsConfiguration;
import com.teragrep.rlp_11.Configuration.TargetConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codahale.metrics.MetricRegistry.name;

public class RelpProbe {

    private static final Logger LOGGER = LoggerFactory.getLogger(RelpProbe.class);
    private final TargetConfiguration targetConfiguration;
    private final RecordFactory recordFactory;
    private final MetricsConfiguration metricsConfiguration;
    private final ProbeConfiguration probeConfiguration;
    private final AtomicBoolean stayRunning = new AtomicBoolean(true);
    private RelpConnection relpConnection;
    private final CountDownLatch latch = new CountDownLatch(1);
    private boolean connected = false;
    private Counter records;
    private Counter resends;
    private Counter connects;
    private Counter disconnects;
    private Counter retriedConnects;
    private Timer sendLatency;
    private Timer connectLatency;
    private final MetricRegistry metricRegistry;

    public RelpProbe(
            final TargetConfiguration targetConfiguration,
            final ProbeConfiguration probeConfiguration,
            final MetricsConfiguration metricsConfiguration,
            final RecordFactory recordFactory,
            final MetricRegistry metricRegistry
    ) {
        this.targetConfiguration = targetConfiguration;
        this.probeConfiguration = probeConfiguration;
        this.metricsConfiguration = metricsConfiguration;
        this.recordFactory = recordFactory;
        this.metricRegistry = metricRegistry;
    }

    public void start() {
        createMetrics(metricsConfiguration.name());
        relpConnection = new RelpConnection();
        connect();
        while (stayRunning.get()) {
            final RelpBatch relpBatch = new RelpBatch();
            relpBatch.insert(recordFactory.createRecord());

            boolean allSent = false;
            while (!allSent && stayRunning.get()) {
                try (final Timer.Context context = sendLatency.time()) {
                    LOGGER.debug("Committing Relpbatch");
                    relpConnection.commit(relpBatch);
                    records.inc();
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
                    resends.inc();
                    relpBatch.retryAllFailed();
                    reconnect();
                }
            }
            try {
                LOGGER.debug("Sleeping before sending next record");
                TimeUnit.MILLISECONDS.sleep(probeConfiguration.interval());
            }
            catch (InterruptedException e) {
                LOGGER.warn("Sleep interrupted: <{}>", e.getMessage());
            }
        }
        disconnect();
        latch.countDown();
    }

    private void connect() {
        while (!connected && stayRunning.get()) {
            try (final Timer.Context context = connectLatency.time()) {
                LOGGER.debug("Connecting to <[{}:{}]>", targetConfiguration.hostname(), targetConfiguration.port());
                connected = relpConnection.connect(targetConfiguration.hostname(), targetConfiguration.port());
                LOGGER.debug("Connected.");
                connects.inc();
            }
            catch (TimeoutException | IOException e) {
                LOGGER
                        .warn(
                                "Failed to connect to <[{}:{}]>: <{}>", targetConfiguration.hostname(),
                                targetConfiguration.port(), e.getMessage()
                        );
            }
            if (!connected) {
                try {
                    LOGGER.debug("Sleeping for <[{}]>ms before reconnecting", targetConfiguration.reconnectInterval());
                    TimeUnit.MILLISECONDS.sleep(targetConfiguration.reconnectInterval());
                    retriedConnects.inc();
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
            LOGGER.debug("Disconnecting..");
            relpConnection.disconnect();
            disconnects.inc();
        }
        catch (IOException | TimeoutException e) {
            LOGGER.warn("Failed to disconnect: <{}>", e.getMessage());
        }
        relpConnection.tearDown();
        LOGGER.debug("Disconnected.");
        connected = false;
    }

    public void stop() {
        LOGGER.debug("Stop called");
        stayRunning.set(false);
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) {
                LOGGER.error("Timed out while waiting for probe to shutdown.");
                throw new RuntimeException("Timed out while waiting for probe to shutdown.");
            }
        }
        catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for latch countdown");
            throw new RuntimeException(e);
        }
        LOGGER.debug("RelpProbe stopped.");
    }

    private void createMetrics(final String name) {
        this.records = metricRegistry.counter(name(RelpProbe.class, "<[" + name + "]>", "records"));
        this.resends = metricRegistry.counter(name(RelpProbe.class, "<[" + name + "]>", "resends"));
        this.connects = metricRegistry.counter(name(RelpProbe.class, "<[" + name + "]>", "connects"));
        this.disconnects = metricRegistry.counter(name(RelpProbe.class, "<[" + name + "]>", "disconnects"));
        this.retriedConnects = metricRegistry.counter(name(RelpProbe.class, "<[" + name + "]>", "retriedConnects"));
        this.sendLatency = metricRegistry
                .timer(name(RelpProbe.class, "<[" + name + "]>", "sendLatency"), () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
        this.connectLatency = metricRegistry
                .timer(name(RelpProbe.class, "<[" + name + "]>", "connectLatency"), () -> new Timer(new SlidingWindowReservoir(metricsConfiguration.window())));
    }
}
