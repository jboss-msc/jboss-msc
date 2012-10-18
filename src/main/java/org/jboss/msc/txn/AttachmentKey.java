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

import org.jboss.msc.value.Factory;

/**
 * A key for a transaction attachment.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@SuppressWarnings("unused")
public final class AttachmentKey<T> {
    private final Factory<T> defaultFactory;

    private AttachmentKey(final Factory<T> factory) {
        defaultFactory = factory;
    }

    private AttachmentKey() {
        defaultFactory = null;
    }

    public static <T> AttachmentKey<T> create() {
        return new AttachmentKey<T>();
    }

    public static <T> AttachmentKey<T> create(Factory<T> factory) {
        return new AttachmentKey<T>(factory);
    }

    T createValue() {
        return defaultFactory == null ? null : defaultFactory.create();
    }
}
