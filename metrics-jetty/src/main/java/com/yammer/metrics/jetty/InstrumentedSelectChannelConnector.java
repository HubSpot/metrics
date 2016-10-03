package com.yammer.metrics.jetty;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

import org.eclipse.jetty.io.AsyncEndPoint;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.nio.AsyncConnection;
import org.eclipse.jetty.server.AsyncHttpConnection;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class InstrumentedSelectChannelConnector extends SelectChannelConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentedSelectChannelConnector.class);
    private static final List<RequestDurationTracker> REQUEST_DURATION_TRACKERS = new CopyOnWriteArrayList<RequestDurationTracker>();
    private final Timer connectionDuration, queueDuration, requestAndQueueDuration;
    private final Meter accepts, connects, disconnects;
    private final Counter connections;

    public InstrumentedSelectChannelConnector(int port) {
        this(Metrics.defaultRegistry(), port);
    }

    public InstrumentedSelectChannelConnector(MetricsRegistry registry,
                                              int port) {
        super();
        setPort(port);
        this.connectionDuration = registry.newTimer(SelectChannelConnector.class,
                                                    "connection-duration",
                                                    Integer.toString(port),
                                                    TimeUnit.MILLISECONDS,
                                                    TimeUnit.SECONDS);
        this.queueDuration = registry.newTimer(SelectChannelConnector.class,
                                           "queue-duration",
                                           Integer.toString(port),
                                           TimeUnit.MILLISECONDS,
                                           TimeUnit.SECONDS);
        this.requestAndQueueDuration = registry.newTimer(SelectChannelConnector.class,
                                                         "request-and-queue-duration",
                                                         Integer.toString(port),
                                                         TimeUnit.MILLISECONDS,
                                                         TimeUnit.SECONDS);
        this.accepts = registry.newMeter(SelectChannelConnector.class,
                                         "accepts",
                                         Integer.toString(port),
                                         "connections",
                                         TimeUnit.SECONDS);
        this.connects = registry.newMeter(SelectChannelConnector.class,
                                          "connects",
                                          Integer.toString(port),
                                          "connections",
                                          TimeUnit.SECONDS);
        this.disconnects = registry.newMeter(SelectChannelConnector.class,
                                             "disconnects",
                                             Integer.toString(port),
                                             "connections",
                                             TimeUnit.SECONDS);
        this.connections = registry.newCounter(SelectChannelConnector.class,
                                               "active-connections",
                                               Integer.toString(port));
    }

    @Override
    public void accept(int acceptorID) throws IOException {
        super.accept(acceptorID);
        accepts.mark();
    }

    @Override
    protected AsyncConnection newConnection(SocketChannel channel, AsyncEndPoint endpoint) {
        return new InstrumentedAsyncHttpConnection(this, endpoint, getServer(), queueDuration, requestAndQueueDuration);
    }

    @Override
    protected void connectionOpened(Connection connection) {
        connections.inc();
        super.connectionOpened(connection);
        connects.mark();
    }

    @Override
    protected void connectionClosed(Connection connection) {
        super.connectionClosed(connection);
        disconnects.mark();
        final long duration = System.currentTimeMillis() - connection.getTimeStamp();
        this.connectionDuration.update(duration, TimeUnit.MILLISECONDS);
        connections.dec();
    }

    private static class InstrumentedAsyncHttpConnection extends AsyncHttpConnection {
        private final Timer queueDuration;
        private final Timer requestAndQueueDuration;
        private final AtomicInteger requests;

        public InstrumentedAsyncHttpConnection(Connector connector,
                                               EndPoint endpoint,
                                               Server server,
                                               Timer queueDuration,
                                               Timer requestAndQueueDuration) {
            super(connector, endpoint, server);
            this.queueDuration = queueDuration;
            this.requestAndQueueDuration = requestAndQueueDuration;
            this.requests = new AtomicInteger();
        }

        @Override
        public Connection handle() throws IOException {
            if (requests.getAndIncrement() != 0) {
                return super.handle();
            }

            long duration = System.currentTimeMillis() - getTimeStamp();
            queueDuration.update(duration, TimeUnit.MILLISECONDS);
            trackQueueDuration(duration);

            Connection connection = super.handle();

            duration = System.currentTimeMillis() - getTimeStamp();
            requestAndQueueDuration.update(duration, TimeUnit.MILLISECONDS);
            trackRequestAndQueueDuration(duration);

            return connection;
        }

        @Override
        public int getRequests() {
            return requests.get();
        }
    }

    /**
     * Register a connection metric tracker to track queue and connection times
     * as they occur.
     * DO NOT BLOCK IN ConnectionMetricTracker CALLS AS THEY WILL BLOCK THE REQUEST THREAD
     * @param requestDurationTracker - The tracker which accepts timings in real time
     */
    public static void registerConnectionMetricTracker(RequestDurationTracker requestDurationTracker) {
        REQUEST_DURATION_TRACKERS.add(requestDurationTracker);
    }

    public static void unregisterConnectionMetricTracker(RequestDurationTracker requestDurationTracker) {
        REQUEST_DURATION_TRACKERS.remove(requestDurationTracker);
    }

    private static void trackQueueDuration(long queueDurationMillis) {
        for (RequestDurationTracker requestDurationTracker : REQUEST_DURATION_TRACKERS) {
            try {
                requestDurationTracker.acceptQueueTime(queueDurationMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                LOGGER.error("Unexpected exception while tracking queue time.", e);
            }
        }
    }

    private static void trackRequestAndQueueDuration(long requestAndQueueDurationMillis) {
        for (RequestDurationTracker requestDurationTracker : REQUEST_DURATION_TRACKERS) {
            try {
                requestDurationTracker.acceptRequestAndQueueTime(requestAndQueueDurationMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                LOGGER.error("Unexpected exception while tracking queue and connection time.", e);
            }
        }
    }
}
