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

package org.jboss.msc.value;

/**
 * An indirect value.  A value may be available trivially (without any computation), or it may involve a complex calculation
 * to produce.  The value may also be <em>time-sensitive</em>, such that it is only available under certain circumstances
 * (e.g. when the corresponding service is "up").
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ReadableValue<T> {

    /**
     * Get the actual value.
     *
     * @return the actual value
     */
    T getValue() throws ValueNotSetException;
}
