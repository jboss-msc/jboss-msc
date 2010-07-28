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

package org.jboss.msc.service;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.TranslatingInjector;
import org.jboss.msc.translate.Translator;
import org.jboss.msc.value.Value;

/**
 * Injection destination that applies a translator to an existing injector.
 * @author John E. Bailey
 */
public class TranslatedInjectionDestination<I, O> extends InjectionDestination {

    private final InjectionDestination delegate;
    private final Translator<I, O> translator;

    public TranslatedInjectionDestination(InjectionDestination delegate, Translator<I, O> translator) {
        this.delegate = delegate;
        this.translator = translator;
    }

    @Override
    protected <T> Injector<?> getInjector(Value<T> injectionValue) {
        final Injector<O> delegateInjector = (Injector<O>) delegate.getInjector(injectionValue);
        return new TranslatingInjector(translator, delegateInjector);
    }
}
