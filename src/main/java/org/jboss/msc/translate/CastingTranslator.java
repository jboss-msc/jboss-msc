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

package org.jboss.msc.translate;

/**
 * A translator which casts (narrows) the type of its input.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class CastingTranslator<I, O> implements Translator<I, O> {
    private final Class<O> type;

    /**
     * Construct a new instance.
     *
     * @param type the type to narrow to
     */
    public CastingTranslator(final Class<O> type) {
        this.type = type;
    }

    /** {@inheritDoc} */
    public O translate(final I input) throws TranslationException {
        try {
            return type.cast(input);
        } catch (ClassCastException e) {
            throw new TranslationException("Input is not of the correct type (expected " + type + ", got " + input.getClass() + ")");
        }
    }
}
