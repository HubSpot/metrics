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

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class InstrumentedSelectChannelConnector extends SelectChannelConnector {
    private final Timer connectionDuration, queueTime, requestDuration;
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
        this.queueTime = registry.newTimer(SelectChannelConnector.class,
                                           "queue-time",
                                           Integer.toString(port),
                                           TimeUnit.MILLISECONDS,
                                           TimeUnit.SECONDS);
        this.requestDuration = registry.newTimer(SelectChannelConnector.class,
                                                 "request-duration",
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
        return new InstrumentedAsyncHttpConnection(this, endpoint, getServer(), queueTime, requestDuration);
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
        private final Timer queueTime;
        private final Timer requestDuration;
        private final AtomicBoolean marked;

        public InstrumentedAsyncHttpConnection(Connector connector,
                                               EndPoint endpoint,
                                               Server server,
                                               Timer queueTime,
                                               Timer requestDuration) {
            super(connector, endpoint, server);
            this.queueTime = queueTime;
            this.requestDuration = requestDuration;
            this.marked = new AtomicBoolean(false);
        }

        @Override
        public Connection handle() throws IOException {
            long requestStart = System.currentTimeMillis();

            if (marked.compareAndSet(false, true)) {
                queueTime.update(requestStart - getTimeStamp(), TimeUnit.MILLISECONDS);
            }

            Connection connection = super.handle();
            requestDuration.update(System.currentTimeMillis() - requestStart, TimeUnit.MILLISECONDS);
            return connection;
        }
    }
}
