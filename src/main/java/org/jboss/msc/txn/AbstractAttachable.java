/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc.txn;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@SuppressWarnings("unchecked")
public abstract class AbstractAttachable implements Attachable {
    private final ConcurrentMap<AttachmentKey<?>, Object> attachments = new ConcurrentHashMap<AttachmentKey<?>, Object>();

    public <T> T getAttachment(final AttachmentKey<T> key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        Object value;
        final ConcurrentMap<AttachmentKey<?>, Object> map = attachments;
        value = map.get(key);
        if (value != null) {
            return (T) value;
        }
        final T newValue = key.createValue();
        final Object appearing = map.putIfAbsent(key, newValue);
        return appearing != null ? (T) appearing : newValue;
    }

    public boolean hasAttachment(final AttachmentKey<?> key) {
        return key != null && attachments.containsKey(key);
    }

    public <T> boolean ensureAttachmentValue(final AttachmentKey<T> key, T expectedValue) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (expectedValue == null) {
            throw new IllegalArgumentException("expectedValue is null");
        }
        T val;
        val = (T) attachments.get(key);
        if (val == null) {
            val = (T) attachments.putIfAbsent(key, expectedValue);
            return val == null || val.equals(expectedValue);
        } else {
            return val.equals(expectedValue);
        }
    }

    public <T> T getAttachmentIfPresent(final AttachmentKey<T> key) {
        return key == null ? null : (T) attachments.get(key);
    }

    public <T> T putAttachment(final AttachmentKey<T> key, final T newValue) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        return (T) attachments.put(key, newValue);
    }

    public <T> T putAttachmentIfAbsent(final AttachmentKey<T> key, final T newValue) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        return (T) attachments.putIfAbsent(key, newValue);
    }

    public <T> T removeAttachment(final AttachmentKey<T> key) {
        return key == null ? null : (T) attachments.remove(key);
    }

    public <T> boolean removeAttachment(final AttachmentKey<T> key, final T expectedValue) {
        return key != null && expectedValue != null && attachments.remove(key, expectedValue);
    }

    public <T> T replaceAttachment(final AttachmentKey<T> key, final T newValue) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        return (T) attachments.replace(key, newValue);
    }

    public <T> boolean replaceAttachment(final AttachmentKey<T> key, final T expectedValue, final T newValue) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (expectedValue == null) {
            throw new IllegalArgumentException("expectedValue is null");
        }
        if (newValue == null) {
            throw new IllegalArgumentException("newValue is null");
        }
        return attachments.replace(key, expectedValue, newValue);
    }
}
