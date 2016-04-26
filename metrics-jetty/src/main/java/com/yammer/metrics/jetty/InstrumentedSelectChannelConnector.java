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

public class InstrumentedSelectChannelConnector extends SelectChannelConnector {
    private final Timer duration, queueTime, queueTimeWithResponseTimeOfFirstRequest;
    private final Meter accepts, connects, disconnects;
    private final Counter connections;

    public InstrumentedSelectChannelConnector(int port) {
        this(Metrics.defaultRegistry(), port);
    }

    public InstrumentedSelectChannelConnector(MetricsRegistry registry,
                                              int port) {
        super();
        setPort(port);
        this.duration = registry.newTimer(SelectChannelConnector.class,
                                          "connection-duration",
                                          Integer.toString(port),
                                          TimeUnit.MILLISECONDS,
                                          TimeUnit.SECONDS);
        this.queueTime = registry.newTimer(SelectChannelConnector.class,
                                           "queue-time",
                                           Integer.toString(port),
                                           TimeUnit.MILLISECONDS,
                                           TimeUnit.SECONDS);
        this.queueTimeWithResponseTimeOfFirstRequest = registry.newTimer(SelectChannelConnector.class,
                                                                         "queue-time-with-response-time-of-first-request",
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
    protected AsyncConnection newConnection(SocketChannel channel,final AsyncEndPoint endpoint) {
        return new InstrumentedAsyncHttpConnection(this, endpoint, getServer());
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
        this.duration.update(duration, TimeUnit.MILLISECONDS);
        connections.dec();
    }

    public class InstrumentedAsyncHttpConnection extends AsyncHttpConnection {

        private boolean marked = false;

        public InstrumentedAsyncHttpConnection(Connector connector, EndPoint endpoint, Server server) {
            super(connector, endpoint, server);
        }

        public Connection handle() throws IOException {
            if (marked) {
                return super.handle();
            }

            marked = true;
            queueTime.update(
                System.currentTimeMillis() - getTimeStamp(),
                TimeUnit.MILLISECONDS);

            Connection connection = super.handle();

            queueTimeWithResponseTimeOfFirstRequest.update(
                System.currentTimeMillis() - getTimeStamp(),
                TimeUnit.MILLISECONDS);

            return connection;
        }

    }

}
