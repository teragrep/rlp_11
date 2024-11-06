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
import com.teragrep.net_01.channel.socket.PlainFactory;
import com.teragrep.net_01.eventloop.EventLoop;
import com.teragrep.net_01.eventloop.EventLoopFactory;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.RelpFrame;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameContext;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConnectionTest {

    private static final int serverPort = 12345;
    private Thread eventLoopThread;
    private EventLoop eventLoop;
    private ExecutorService executorService;
    private final List<String> records = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionTest.class);

    @BeforeEach
    public void StartServer() {
        executorService = Executors.newFixedThreadPool(1);
        Consumer<FrameContext> syslogConsumer = new Consumer<>() {

            @Override
            public synchronized void accept(FrameContext frameContext) {
                try (RelpFrame relpFrame = frameContext.relpFrame()) {
                    records.add(relpFrame.payload().toString());
                }
            }
        };
        Supplier<FrameDelegate> frameDelegateSupplier = () -> new DefaultFrameDelegate(syslogConsumer);
        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        eventLoop = Assertions.assertDoesNotThrow(eventLoopFactory::create);
        eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();
        ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                executorService,
                new PlainFactory(),
                new FrameDelegationClockFactory(frameDelegateSupplier)
        );
        Assertions.assertDoesNotThrow(() -> serverFactory.create(serverPort));
    }

    @AfterEach
    public void StopServer() {
        eventLoop.stop();
        Assertions.assertDoesNotThrow(() -> eventLoopThread.join());
        executorService.shutdown();
        records.clear();
    }

    @Test
    public void ConnectToServerTest() {
        RelpProbeConfiguration relpProbeConfiguration = Assertions
                .assertDoesNotThrow(
                        () -> new RelpProbeConfiguration(
                                new PathConfiguration("src/test/resources/connectiontest.properties").asMap()
                        )
                );
        RelpProbe relpProbe = new RelpProbe(relpProbeConfiguration);

        TimerTask task = new TimerTask() {

            public void run() {
                relpProbe.stop();
            }
        };
        Timer timer = new Timer("Timer");
        timer.schedule(task, 5000L);

        relpProbe.start();

        LOGGER.info("Got records: {}", records);
    }
}
