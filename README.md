JBoss Modular Service Container (JBoss MSC)

JBoss MSC is a lightweight highly concurrent dependency injection container for Java. It is built on three key abstractions: "Service", "Service controller" and "Service container".
Every service has two lifecycle phases - "start phase" and "stop phase". Services can start and stop either "synchronously" or "asynchronously".
Every service once successfully installed into service container has associated service controller with it. Service controllers are useful for adding / removing lifecycle listeners to / from services at runtime
or can be used to retrieve some information about service such as its name, mode, state or unavailable dependencies.
Service container provides information about installed services and provides methods for retrieving service controllers associated with those services.

## Building

Prerequisites:

* JDK 11 or newer - check `java -version`
* Maven 3.6.0 or newer - check `mvn -v`

To build with your own Maven installation:

    mvn install

## Documentation

All documentation lives at http://jboss-msc.github.io/jboss-msc/manual/

## Issue tracker

All issues can be reported at https://issues.jboss.org/browse/MSC

## Code

All code can be found at https://github.com/jboss-msc/jboss-msc

## License

All code distributed under [GNU Lesser General Public License Version 2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html).
