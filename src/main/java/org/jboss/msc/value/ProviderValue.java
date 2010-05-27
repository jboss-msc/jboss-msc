package org.jboss.msc.value;

import javax.inject.Provider;

/**
 * A value which creates an instance via a {@link Provider}.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProviderValue<T> implements Value<T> {
    private final Provider<T> provider;

    /**
     * Create a new instance.
     *
     * @param provider the provider to use
     */
    public ProviderValue(final Provider<T> provider) {
        this.provider = provider;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        return provider.get();
    }
}
