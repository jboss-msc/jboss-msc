package org.jboss.msc.registry;

/**
 * @author John Bailey
 */
public class ServiceName {
    private final String name;
    private final int hashCode;

    public ServiceName(String name) {
        this.name = name;
        hashCode = name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceName that = (ServiceName) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return name;
    }
}
