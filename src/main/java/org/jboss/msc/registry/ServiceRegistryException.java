package org.jboss.msc.registry;

/**
 * @author John Bailey
 */
public class ServiceRegistryException extends Exception {
    public ServiceRegistryException() {
    }

    public ServiceRegistryException(Throwable cause) {
        super(cause);
    }

    public ServiceRegistryException(String message) {
        super(message);
    }

    public ServiceRegistryException(String message, Throwable cause) {
        super(message, cause);
    }
}
