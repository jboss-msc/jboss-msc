/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.msc.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A mapper for service names which allows a service name or pattern to be correlated with a value.  If more than one
 * pattern matches a service name, the best match is chosen; if there are multiple best matches then an arbitrary one
 * is selected.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceNameMapper<V> {

    /**
     * The special marker which represents a pattern match.
     */
    public static final Object ANY = new Object();

    private final Node<V> root = new Node<V>();

    /**
     * Register a matcher for a specific location.
     *
     * @param value the value to register
     * @param segments the segments to match
     */
    public void registerMatcher(V value, Object... segments) {
        doRegisterMatcher(segments, 0, value, root);
    }

    private void doRegisterMatcher(final Object[] segments, final int idx, final V value, final Node<V> current) {
        if (segments.length == idx) {
            if (! Node.valueUpdater.compareAndSet(current, null, value)) {
                throw new IllegalArgumentException("Matcher already registered");
            }
            return;
        }
        doRegisterMatcher(segments, idx + 1, value, current.getOrCreateChild(segments[idx]));
    }

    /**
     * Attempt to match a service name.
     *
     * @param serviceName the service name
     * @return the value result, or {@code null} if no match was found
     */
    public V match(ServiceName serviceName) {
        return match(serviceName.toArray(), 0, root);
    }

    private V match(final String[] segments, final int idx, final Node<V> current) {
        if (segments.length == idx) {
            return current.value;
        }
        String segment = segments[idx];
        Node<V> childNode = current.childMap.get(segment);
        if (childNode != null) {
            V result = match(segments, idx + 1, childNode);
            if (result != null) {
                return result;
            }
        }
        childNode = current.childMap.get(ANY);
        if (childNode != null) {
            return match(segments, idx + 1, childNode);
        }
        return null;
    }

    static final class Node<V> {

        @SuppressWarnings("unchecked")
        private static final AtomicReferenceFieldUpdater<Node, Object> valueUpdater = AtomicReferenceFieldUpdater.newUpdater(Node.class, Object.class, "value");

        private final ConcurrentMap<Object, Node<V>> childMap = new ConcurrentHashMap<Object, Node<V>>();
        @SuppressWarnings("unused")
        private volatile V value;

        Node<V> getOrCreateChild(Object key) {
            if (key == null) {
                throw new IllegalArgumentException("Null segment encountered");
            }
            if (key != ANY) {
                key = key.toString();
            }
            Node<V> child = childMap.get(key);
            if (child == null) {
                Node<V> appearing = childMap.putIfAbsent(key, child = new Node<V>());
                if (appearing != null) {
                    child = appearing;
                }
            }
            return child;
        }
    }
}
