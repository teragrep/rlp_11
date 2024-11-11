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
package com.teragrep.rlp_11.metrics;

import com.codahale.metrics.MetricRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;

public class HttpReport implements Report {

    private final Server jettyServer;
    private final MetricRegistry metricRegistry;

    public HttpReport(final MetricRegistry metricRegistry, final int prometheusPort) {
        this.metricRegistry = metricRegistry;
        jettyServer = new Server(prometheusPort);
    }

    @Override
    public void start() {
        // prometheus-exporter
        CollectorRegistry.defaultRegistry.register(new DropwizardExports(metricRegistry));

        final ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        jettyServer.setHandler(context);

        final MetricsServlet metricsServlet = new MetricsServlet();
        final ServletHolder servletHolder = new ServletHolder(metricsServlet);
        context.addServlet(servletHolder, "/metrics");

        // Start the webserver.
        try {
            jettyServer.start();
        }
        //CHECKSTYLE:OFF
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        //CHECKSTYLE:ON
    }

    @Override
    public void close() throws IOException {
        try {
            jettyServer.stop();
        }
        //CHECKSTYLE:OFF
        catch (Exception e) {
            throw new IOException(e);
        }
        //CHECKSTYLE:ON
    }
}
