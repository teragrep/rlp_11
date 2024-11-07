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
import com.teragrep.net_01.server.Server;
import com.teragrep.net_01.server.ServerFactory;
import com.teragrep.rlp_03.frame.FrameDelegationClockFactory;
import com.teragrep.rlp_03.frame.delegate.DefaultFrameDelegate;
import com.teragrep.rlp_03.frame.delegate.FrameDelegate;
import com.teragrep.rlp_11.Configuration.ProbeConfiguration;
import com.teragrep.rlp_11.Configuration.MetricsConfiguration;
import com.teragrep.rlp_11.Configuration.PrometheusConfiguration;
import com.teragrep.rlp_11.Configuration.TargetConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ConnectionTest {

    private final int serverPort = 12345;
    private Thread eventLoopThread;
    private EventLoop eventLoop;
    private ThreadPoolExecutor threadPoolExecutor;
    private final List<String> records = new ArrayList<>();
    private Server server;

    @BeforeEach
    public void startServer() {
        EventLoopFactory eventLoopFactory = new EventLoopFactory();
        eventLoop = Assertions.assertDoesNotThrow(eventLoopFactory::create);

        eventLoopThread = new Thread(eventLoop);
        eventLoopThread.start();

        Supplier<FrameDelegate> frameDelegateSupplier = () -> new DefaultFrameDelegate((frameContext) -> {
            // Adds random latency before finishing and acking the event
            Assertions.assertDoesNotThrow(() -> Thread.sleep((long) (Math.random() * 500)));
            records.add(frameContext.relpFrame().payload().toString());
        });

        threadPoolExecutor = new ThreadPoolExecutor(
                1,
                1,
                Long.MAX_VALUE,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
        ServerFactory serverFactory = new ServerFactory(
                eventLoop,
                threadPoolExecutor,
                new PlainFactory(),
                new FrameDelegationClockFactory(frameDelegateSupplier)
        );
        server = Assertions.assertDoesNotThrow(() -> serverFactory.create(serverPort));
    }

    @AfterEach
    public void stopServer() {
        eventLoop.stop();
        threadPoolExecutor.shutdown();
        Assertions.assertDoesNotThrow(() -> eventLoopThread.join());
        Assertions.assertDoesNotThrow(server::close);
        records.clear();
    }

    @Test
    public void connectToServerTest() {
        Map<String, String> map = Assertions
                .assertDoesNotThrow(() -> new PathConfiguration("src/test/resources/connectiontest.properties").asMap());
        final PrometheusConfiguration prometheusConfiguration = new PrometheusConfiguration(map);
        final ProbeConfiguration probeConfiguration = new ProbeConfiguration(map);
        final RecordFactory recordFactory = new RecordFactory("localhost", "rlp_11", "rlp_11");
        final TargetConfiguration targetConfiguration = new TargetConfiguration(map);
        final MetricsConfiguration metricsConfiguration = new MetricsConfiguration(map);

        RelpProbe relpProbe = new RelpProbe(
                targetConfiguration,
                probeConfiguration,
                prometheusConfiguration,
                metricsConfiguration,
                recordFactory
        );

        TimerTask task = new TimerTask() {

            public void run() {
                relpProbe.stop();
            }
        };
        Timer timer = new Timer("Timer");
        timer.schedule(task, 5_000L);

        relpProbe.start();
    }
}
