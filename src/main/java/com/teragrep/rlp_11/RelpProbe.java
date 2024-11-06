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

import com.teragrep.rlo_14.Facility;
import com.teragrep.rlo_14.Severity;
import com.teragrep.rlo_14.SyslogMessage;
import com.teragrep.rlp_01.RelpBatch;
import com.teragrep.rlp_01.RelpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RelpProbe {

    private final RelpProbeConfiguration config;
    private boolean stayRunning = true;
    private RelpConnection relpConnection;
    private final CountDownLatch latch = new CountDownLatch(1);
    private static final Logger LOGGER = LoggerFactory.getLogger(RelpProbe.class);
    private boolean connected = false;

    public RelpProbe(RelpProbeConfiguration config) {
        this.config = config;
    }

    public void start() {
        relpConnection = new RelpConnection();
        connect();
        int eventDelay = config.getEventDelay();
        while (stayRunning) {
            RelpBatch relpBatch = new RelpBatch();
            Instant timestamp = Instant.now();
            byte[] record = new SyslogMessage()
                    .withTimestamp(timestamp.toEpochMilli())
                    .withAppName(config.getEventAppname())
                    .withHostname(config.getEventHostname())
                    .withFacility(Facility.USER)
                    .withSeverity(Severity.INFORMATIONAL)
                    .withMsg(timestamp.getEpochSecond() + "." + timestamp.getNano())
                    .toRfc5424SyslogMessage()
                    .getBytes(StandardCharsets.UTF_8);
            relpBatch.insert(record);

            boolean allSent = false;
            while (!allSent) {
                try {
                    relpConnection.commit(relpBatch);
                }
                catch (IllegalStateException | IOException | java.util.concurrent.TimeoutException e) {
                    LOGGER.warn("Failed to commit: <{}>", e.getMessage());
                    relpConnection.tearDown();
                    connected = false;
                }
                allSent = relpBatch.verifyTransactionAll();
                if (!allSent) {
                    relpBatch.retryAllFailed();
                    reconnect();
                }
            }
            try {
                Thread.sleep(eventDelay);
            }
            catch (InterruptedException e) {
                LOGGER.warn("Sleep interrupted: <{}>", e.getMessage());
            }
        }

    }

    private void connect() {
        while (!connected && stayRunning) {
            try {
                connected = relpConnection.connect(config.getTargetHostname(), config.getTargetPort());
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
                    Thread.sleep(config.getReconnectInterval());
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
            return;
        }
        try {
            relpConnection.disconnect();
        }
        catch (IOException | TimeoutException e) {
            LOGGER.warn("Failed to disconnect: <{}>", e.getMessage());
        }
        relpConnection.tearDown();
        connected = false;
    }

    public void stop() {
        disconnect();
        stayRunning = false;
        try {
            if (!latch.await(5L, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timed out while waiting for probe to shutdown.");
            }
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
