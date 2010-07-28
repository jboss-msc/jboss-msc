/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.lang.reflect.Method;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * An HTTP context.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class HttpContextService implements Service<HttpContext> {
    private String path;
    private HttpHandler handler;
    private HttpServer server;
    private HttpContext httpContext;

    public static final Value<Method> SERVER_SETTER = Values.getSetterMethod("server", HttpServer.class);
    public static final Value<Method> HANDLER_SETTER = Values.getSetterMethod("handler", HttpHandler.class);
    public static final Value<Method> PATH_SETTER = Values.getSetterMethod("path", String.class);

    public final Injector<HttpServer> httpServerInjector = SetMethodInjector.create(Values.immediateValue(this), SERVER_SETTER);
    public final Injector<HttpHandler> httpHandlerInjector = SetMethodInjector.create(Values.immediateValue(this), HANDLER_SETTER);
    public final Injector<String> pathInjector = SetMethodInjector.create(Values.immediateValue(this), PATH_SETTER);

    public synchronized String getPath() {
        return path;
    }

    public synchronized void setPath(final String path) {
        this.path = path;
    }

    public synchronized HttpHandler getHandler() {
        return handler;
    }

    public synchronized void setHandler(final HttpHandler handler) {
        this.handler = handler;
    }

    public synchronized HttpServer getServer() {
        return server;
    }

    public synchronized void setServer(final HttpServer server) {
        this.server = server;
    }

    public synchronized void start(final StartContext context) throws StartException {
        final HttpHandler handler = this.handler;
        if (handler == null) {
            throw new StartException("handler is null");
        }
        final HttpServer server = this.server;
        if (server == null) {
            throw new StartException("server is null");
        }
        final String path = this.path;
        if (path == null) {
            throw new StartException("path is null");
        }
        httpContext = server.createContext(path, handler);
    }

    public synchronized void stop(final StopContext context) {
        final HttpServer server = this.server;
        final HttpContext httpContext = this.httpContext;
        server.removeContext(httpContext);
        this.httpContext = null;
    }

    public synchronized HttpContext getValue() throws IllegalStateException {
        final HttpContext context = httpContext;
        if (context == null) {
            throw new IllegalStateException("Context not started");
        }
        return context;
    }
}
