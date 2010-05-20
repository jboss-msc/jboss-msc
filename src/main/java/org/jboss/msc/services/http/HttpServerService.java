/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.services.http;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * An example service using the embedded JDK HTTP server found in some Java distributions.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class HttpServerService implements Service<HttpServer> {
    private HttpServer realServer;
    private HttpServer server;
    private Executor executor;
    private int stopDelay = 5;
    private int backlog = 0;
    private InetSocketAddress bindAddress;

    public static final Value<Method> EXECUTOR_SETTER = Values.getSetterMethod("executor", Executor.class);

    public final Injector<Executor> executorInjector = new SetMethodInjector<Executor>(new ImmediateValue<Value<?>>(this), EXECUTOR_SETTER);

    public synchronized Executor getExecutor() {
        return executor;
    }

    public synchronized void setExecutor(final Executor executor) {
        this.executor = executor;
    }

    public synchronized int getStopDelay() {
        return stopDelay;
    }

    public synchronized void setStopDelay(final int stopDelay) {
        if (stopDelay < 0) {
            throw new IllegalArgumentException("Bad value for stop delay (must be >0)");
        }
        this.stopDelay = stopDelay;
    }

    public synchronized void start(final StartContext context) throws StartException {
        final Executor executor = this.executor;
        final HttpServer server;
        try {
            server = HttpServer.create();
            server.setExecutor(executor);
            server.bind(bindAddress, backlog);
            server.start();
        } catch (IOException e) {
            throw new StartException("Failed to start web server", e);
        }
        this.server = new HttpServerWrapper(server);
        realServer = server;
    }

    public synchronized void stop(final StopContext context) {
        realServer.stop(stopDelay);
    }

    public synchronized int getBacklog() {
        return backlog;
    }

    public synchronized void setBacklog(final int backlog) {
        this.backlog = backlog;
    }

    public synchronized InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    public synchronized void setBindAddress(final InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;
    }

    public synchronized HttpServer getValue() throws IllegalStateException {
        final HttpServer server = this.server;
        if (server == null) throw new IllegalStateException("Not started");
        return server;
    }

    private class HttpServerWrapper extends HttpServer {
        private final HttpServer delegate;

        private HttpServerWrapper(final HttpServer delegate) {
            this.delegate = delegate;
        }

        public HttpContext createContext(final String s, final HttpHandler httpHandler) {
            return delegate.createContext(s, httpHandler);
        }

        public HttpContext createContext(final String s) {
            return delegate.createContext(s);
        }

        public void removeContext(final String s) throws IllegalArgumentException {
            throw new UnsupportedOperationException("remove context by path");
        }

        public void removeContext(final HttpContext httpContext) {
            delegate.removeContext(httpContext);
        }

        public InetSocketAddress getAddress() {
            return delegate.getAddress();
        }

        public void bind(final InetSocketAddress inetSocketAddress, final int i) throws IOException {
            throw new UnsupportedOperationException();
        }

        public void start() {
            throw new UnsupportedOperationException();
        }

        public void setExecutor(final Executor executor) {
            throw new UnsupportedOperationException();
        }

        public Executor getExecutor() {
            return delegate.getExecutor();
        }

        public void stop(final int i) {
            throw new UnsupportedOperationException();
        }
    }
}
