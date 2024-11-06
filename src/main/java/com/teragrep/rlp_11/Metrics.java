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

import static com.codahale.metrics.MetricRegistry.name;

public class Metrics {

    public final Counter records;
    public final Counter resends;
    public final Counter connects;
    public final Counter disconnects;
    public final Counter retriedConnects;
    public final Timer sendLatency;
    public final Timer connectLatency;
    public final MetricRegistry metricRegistry;

    public Metrics(final String name) {
        this.metricRegistry = new MetricRegistry();
        this.records = metricRegistry.counter(name(Metrics.class, "<[" + name + "]>", "records"));
        this.resends = metricRegistry.counter(name(Metrics.class, "<[" + name + "]>", "resends"));
        this.connects = metricRegistry.counter(name(Metrics.class, "<[" + name + "]>", "connects"));
        this.disconnects = metricRegistry.counter(name(Metrics.class, "<[" + name + "]>", "disconnects"));
        this.retriedConnects = metricRegistry.counter(name(Metrics.class, "<[" + name + "]>", "retriedConnects"));
        this.sendLatency = metricRegistry
                .timer(name(Metrics.class, "<[" + name + "]>", "sendLatency"), () -> new Timer(new SlidingWindowReservoir(10000)));
        this.connectLatency = metricRegistry
                .timer(name(Metrics.class, "<[" + name + "]>", "connectLatency"), () -> new Timer(new SlidingWindowReservoir(10000)));
    }
}
