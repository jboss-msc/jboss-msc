# About JBoss MSC

## Introduction

**JBoss MSC** (JBoss Modular Service Container) is a lightweight highly concurrent  
dependency injection container for [Java](https://www.oracle.com/java/)  
It is developed under the [LGPL 2.1](../LICENSE.txt) license.  
The latest API documentation is available [here](../apidocs/index.html).

## Basic Concepts

**JBoss MSC** is built on three key abstractions.

### Service

[Service](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/Service.html) must be installed into
[Service Container](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceContainer.html) to become functional.  
Every service has two lifecycle phases - _start phase_ and _stop phase_.  
Services can start and stop either _synchronously_ or _asynchronously_.  
Furthermore every service has:  

- _dependencies_ - service may require values provided by other services
- _dependents_ - service may provide values to other services
- _mode_ - defines service's starting policy
- _state_ - current state of the service

## Service Controller

Every [Service](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/Service.html) successfully installed
into [Service Container](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceContainer.html)
has associated [Service Controller](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceController.html) with it.  
Service controllers are useful for adding / removing [Lifecycle Listener](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/LifecycleListener.html)s to / from service at runtime,  
they can be used to change service mode at runtime (this includes also scheduling service for removal)  or to retrieve some useful  
information about service such as _provided values_, _required values_, _mode_, _state_ and so on.

## Service Container

Every successfully installed service is stored in [Service Container](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceContainer.html).  
Service container provides information about available services and provides accessors  
for retrieving service controllers associated with installed services.  
Service container also provides _shutdown_ mechanism to stop it.

# Getting Started with JBoss MSC

## Introduction

This chapter will guide you through the process of setting up and starting a JBoss MSC container with one service.

## Prerequisities

Before you begin, ensure that you have the following prerequisites in place:

- [Java Development Kit (JDK)](https://www.oracle.com/java/technologies/downloads/): JBoss MSC requires a compatible JDK to run. Ensure you have Java 11 or later installed on your system.

- [Maven (Optional)](https://maven.apache.org/download.cgi): If you intend to manage your project using Maven, make sure it is installed. Maven simplifies the build and dependency management process.

## Setting Up a JBoss MSC Project

Follow these steps to set up a new JBoss MSC project:

1. **Create a New Project Directory**: Create a new directory for your project. You can name it whatever you like.
2. **Create a Maven Project (Optional)**: If you're using Maven, navigate to the project directory and run the following command in shell to create a new Maven project:

```
mvn archetype:generate -DgroupId=com.example -DartifactId=my-jboss-msc-app -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

3. **Add JBoss MSC Dependency**: To include JBoss MSC in your project, add the following dependency to your pom.xml file if you're using Maven:

```
<dependency>
    <groupId>org.jboss.msc</groupId>
    <artifactId>jboss-msc</artifactId>
    <version>1.5.0.Final</version> <!-- Replace with the latest version -->
</dependency>
```

4. **Create Your JBoss MSC Services**: Create the Java classes that represent your services and dependencies. These classes should implement the [org.jboss.msc.Service](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/Service.html) interface.

5. **Install JBoss MSC Services into ServiceContainer**: When installing services into Service Container

6. **Write a Main Class**: Create a main class that will bootstrap and start your JBoss MSC container and install your services into it.  
This class should create an instance of [Service Container](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceContainer.html) and add your services to it.

To install Services into Service Container you must first create the ServiceBuilder instance via [ServiceContainer.addService()](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceTarget.html#addService()) method and then call the following methods on it:

- [ServiceBuilder.requires()](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceBuilder.html#requires(org.jboss.msc.service.ServiceName)) method to define values your service requires from other services
- [ServiceBuilder.provides()](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceBuilder.html#provides(org.jboss.msc.service.ServiceName...)) method to define values your service provides to other services
- [ServiceBuilder.setInitialMode()](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceBuilder.html#setInitialMode(org.jboss.msc.service.ServiceController.Mode)) to specify the service startup mode of your services
- [ServiceBuilder.setInstance()](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceBuilder.html#setInstance(org.jboss.msc.Service)) to set the service instance to be managed by the Service Container
- And finally call [ServiceBuilder.install()](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceBuilder.html#install()) to install the service into the Service Container.

## Starting the JBoss MSC Container

Once your project is set up and your services are defined, you can start the JBoss MSC container by following these steps:

- **Create a ServiceContainer**: In your main class, create an instance of org.jboss.msc.ServiceContainer:

```
ServiceContainer container = ServiceContainer.Factory.create();
```

- **Start the Container**: Start the container to prepare executing your services:

```
container.start();
```

- **Add Your Services**: Add your service instances to the container:

```
container.addService().setInstance(new MyService()).install();
```

Replace MyService with the actual name of your service.

- **Stop the Container (Optional)**: To gracefully shut down the container and stop your services, use the following code:

```
container.stop();
```

## Conclusion

Congratulations! You have successfully set up and started a JBoss MSC container for managing your modular services.  
JBoss MSC provides a robust framework for managing service lifecycles and dependencies within your Java application.  
You can now build complex applications with ease, taking advantage of the powerful features offered by JBoss MSC.

# Configuring Services with ServiceBuilder and ServiceController Methods

In JBoss MSC, the ServiceBuilder class provides advanced configuration options for your services.  
You can use various ServiceBuilder methods to specify characteristics, dependencies, and behaviors of your services.  
This chapter will explore some of the most commonly used ServiceBuilder methods for advanced service configuration.

## Dependencies Between Services

### Specifying Required Values from Other Services

Your services may require values that are provided by other services.

- **requires()**: Use this method to declare values your services require from other services.
When a service depends on a values from another services, it won't start until its dependencies are satisfied.

```
ServiceBuilder<?> sb = container.addService();
Supplier<Integer> httpPort = sb.requires(ServiceName.of("server.config.http.port");
sb.setInstance(new HttpServerService(httpPort));
sb.install();
```

### Specifying Provided Values to Other Services

Your services may provide values that are required by other services.

- **provides()**: Use this method to declare values you service provide to other services.

```
ServiceBuilder<?> sb = container.addService();
Consumer<Integer> httpPort = sb.provides(ServiceName.of("server.config.http.port");
sb.setInstance(new HttpServerConfigService(httpPort));
sb.install();
```

## Lifecycle Configuration

Every service has a [bootstrap mode](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceController.Mode.html) associated with it. If there is no configuration change then by default the service mode is always **Mode.ACTIVE**.

- **setInitialMode()**: Configure the initial mode of a service (e.g., Mode.ACTIVE, Mode.ON_DEMAND, Mode.NEVER). The service will start in the specified mode in the container.

```
ServiceBuilder<?> sb = container.addService();
sb.setIntialMode(Mode.LAZY); // if this method isn't called at all then default mode is always Mode.ACTIVE
sb.setInstance(new LazyService());
sb.install();
```

Every service at any point of time is in one of its [internal states](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceController.State.html).

- **addListener()**: Users can attach listeners to services to monitor their lifecycle events. Listeners can perform actions before, during, or after service startup and shutdown.

```
ServiceBuilder<?> sb = container.addService();
sb.addListener(new ServiceStateMonitoringListener());
sb.setInstance(new MonitoredService());
sb.install();
```

or

```
ServiceBuilder<?> sb = container.addService();
sb.setInstance(new MonitoredService());
ServiceController<?> ctrl = sb.install();
// ...
// after some time
// ...
ctrl.addListener(new ServiceStateMonitoringListener());
```

- **removeListener()**: Once listeners have completed their job it is possible to remove them from the service via its Service Controller.

```
ServiceBuilder<?> sb = container.addService();
LifecycleListener listener = new ServiceStateMonitoringListener();
sb.addListener(listener);
sb.setInstance(new MonitoredService());
ServiceController<?> ctrl = sb.install();
// ...
// after some time
// ...
ctrl.removeListener(listener);
```

## Service Removal

- **setMode()**: In order to remove installed services from Service Container users have to call [ServiceController.setMode()](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceController.html#setMode(org.jboss.msc.service.ServiceController.Mode)) with _Mode.REMOVE_ parameter.

```
ServiceBuilder<?> sb = container.addService();
sb.addListener(new ServiceStateMonitoringListener());
sb.setInstance(new MonitoredService());
ServiceController<?> ctrl = sb.install();
// ...
// after some time
// ...
ctrl.setMode(Mode.REMOVE);
```

## Conclusion

Understanding and utilizing the ServiceBuilder methods in JBoss MSC provides you with fine-grained control over service configuration, dependencies, and behavior.
These methods allow you to tailor your services to meet specific requirements and manage their lifecycles effectively.
By exploring these advanced features, you can create robust and highly configurable applications using JBoss MSC.

# Implementing Service

In JBoss MSC (Modular Service Container), implementing [Service](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/Service.html) involves defining a service that encapsulates specific functionality or components within your application. This chapter will guide you through the process of implementing a service that accepts _java.util.function.Consumer_ for provided values by the service and _java.util.function.Supplier_ for values the service depends on, while ensuring thread safety and adhering to JBoss MSC's guarantees.

## Service Implementation

Let's create a sample service named HttpServerService that provides a basic example of how to implement the _org.jboss.msc.Service_ interface:

```
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class HttpServerService implements Service {

    private final Supplier<Integer> httpPortSupplier;
    private final Consumer<HttpServer> serverProvider;

    public HttpServerService(Supplier<Integer> httpPortSupplier, Consumer<HttpServer> server) {
        this.httpPortSupplier = httpPortSupplier;
        this.serverProvider = serverProvider;
    }

    @Override
    public void start(StartContext context) {
        // This method is called when the service is started, and all required values (dependencies) are guaranteed to be available.
        Integer httpPort = httpPortSupplier.get();
        serverProvider.accept(new HttpServer(httpPort));

        // Perform other initialization or start-up tasks here.
    }

    @Override
    public void stop(StopContext context) {
        // This method is called when the service is stopped, required values provided by other services are still guaranteed to be available.
        serverProvider.accept(null);

        // Perform other cleanup or shut-down tasks here.
    }
}
```

In this example:

- *HttpServerService* accepts a Supplier<Integer> for values it depends on and a Consumer<HttpServer> for provided values.
- The _start()_ method is invoked when the service is started, and it is guaranteed that the required dependencies are up at this point. You can safely access and use the dependencies within this method.
- The _stop()_ method is called when the service is stopped, and it is guaranteed that the dependencies are still up. You can safely perform cleanup or shutdown tasks in this method.

## Service Registration

Once you've implemented your service, you need to register it with the ServiceContainer. Here's how you can do it:

```
ServiceContainer container = ServiceContainer.Factory.create();
ServiceBuilder<?> sb = container.addService();
Supplier<Integer> portSupplier = sb.requires(ServiceName.of("server.config.http.port"));
Consumer<HttpServer> serverProvider = sb.provides(ServiceName.of("http.server.instance"));
HttpServerService service = new HttpServerService(portSupplier, serverProvider);
sb.setInstance(service);
sb.install();
```

In this code:

- _server.config.http.port_ is the name of the value provided by other service this _HttpServerService_ depends on.
- _http.server.instance_ is the name of provided value of _HttpServerService_.
- The _HttpServerService_ instance is created and installed.

## Thread Safety and Guarantees

JBoss MSC provides guarantees regarding thread safety during service start() and stop() methods:

- In the _start()_ method, you can safely access and use your dependencies, as it is guaranteed that they are up and available.
- In the _stop()_ method, it is also guaranteed that the dependencies are still up, allowing you to perform cleanup or shutdown tasks without concerns about the dependencies being unavailable.

By following these guidelines, you can create well-behaved services in JBoss MSC that safely manage their dependencies and adhere to the container's lifecycle guarantees.

## Conclusion

Implementing [Service](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/Service.html) in JBoss MSC allows you to encapsulate specific functionality or components within your application in a modular and organized manner. By accepting _java.util.function.Consumer_ for provided values and _java.util.function.Supplier_ for required values, you can build services that are flexible and easy to configure. Understanding thread safety and adhering to JBoss MSC's guarantees ensures the reliable operation of your services within the container.

# Understanding StartContext and StopContext

[Start context](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/StartContext.html) and [Stop context](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/StopContext.html)
are important components in JBoss MSC that provide valuable capabilities during the start-up and shutdown phases of services. They offer methods for synchronous and asynchronous service installation, and proper usage of these contexts can greatly enhance the flexibility and control you have over your services.

## StartContext and Synchronous Service Installation

The _StartContext_ represents the context in which a service's _start()_ method is executed. It offers methods for installing services either _synchronously_ or _asynchronously_. Let's explore synchronous service installation using the _complete()_ and _fail()_ methods:

### Synchronous Start Using complete() or fail() Methods Explicitly

```
@Override
public void start(StartContext context) {
    try {
        // Perform initialization tasks.

        // Synchronously complete service installation when ready.
        context.complete();
    } catch (Exception e) {
        // Or, if an error occurs during startup, fail the installation.
        context.fail(new StartException("Service failed to start", e));
    }
}
```
- The _context.complete()_ method indicates that the service has successfully started, allowing the container to proceed with other services that depend on it.
- Conversely, the _context.fail()_ method can be used to indicate that the service failed to start, and it will prevent dependent services from starting.

### Synchronous Start Using complete() or fail() Methods Implicitly

```
@Override
public void start(StartContext context) throws StartException {
    try {
        // Perform initialization tasks.

        // When this method terminates here normally without throwing exception JBoss MSC container
        // will call context.complete() internally on behalf of the user to indicate successful installation.
    } catch (Exception e) {
        throw new StartException("Service failed to start", e);
        // Or, if an error occurs during startup and either RuntimeException or subclass of StartException is thrown from this method
        // the context.fail() is called internally by JBoss MSC container on behalf of the user to indicate failed installation of this service.
    }
}
```

## StartContext and Asynchronous Service Installation

In scenarios where your service's _start()_ method includes asynchronous operations, you can use the _asynchronous()_ method to inform the container that the service installation is in progress. 
To complete or fail the service installation asynchronously, you need to manage parallel execution using additional threads:

### Asynchronous Start

```
@Override
public void start(StartContext context) throws StartException {
    // Perform asynchronous initialization tasks.
    CompletableFuture<Void> asyncInitialization = performAsyncInitialization();

    // Indicate that service installation is in progress.
    context.asynchronous();

    // Use a separate thread to complete or fail the service installation when asynchronous code is complete.
    asyncInitialization.whenCompleteAsync((result, throwable) -> {
        if (throwable != null) {
            // Fail the service installation if an exception occurred.
            context.fail(throwable);
        } else {
            // Complete the service installation when ready.
            context.complete();
        }
    });
}
```

- The _context.asynchronous()_ method informs the container that service installation is in progress and allows other services to start independently and use the current JBoss MSC execution thread.
- Asynchronous initialization tasks are performed using a _CompletableFuture_ or similar asynchronous mechanism.
- A separate thread or parallel execution is used to monitor the completion of asynchronous code.
- When the asynchronous code is complete, either _context.fail()_ or _context.complete()_ is called accordingly.

## StopContext

Similarly, _StopContext_ is used during service shutdown to provide control over the shutdown process. You can use it to indicate that the service is stopping and, if necessary, manage asynchronous shutdown tasks.

### Synchronous Stop Using complete() Method Explicitly

```
@Override
public void stop(StopContext context) {
    try {
        // Perform cleanup tasks.

        // Synchronously complete service shutdown when ready.
        context.complete();
    } catch (Exception e) {
        // Or, if an error occurs during shutdown and because the stop() method can never fail in JBoss MSC, log the incident to the system console
        Logger.log("stop method failed", throwable);
    }
}
```
- The _context.complete()_ method indicates that the service has successfully stopped, allowing the container to proceed with other services that depend on it.
- Conversely, the shutdown failure is logged to the system console (because JBoss MSC service's stop() method can never fail) to indicate that the service failed to stop.

### Synchronous Stop Using complete() Method Implicitly

```
@Override
public void stop(StopContext context) {
    // Perform cleanup tasks.

    // When this method terminates here normally without throwing RuntimeException JBoss MSC container
    // will call context.complete() internally on behalf of the user to indicate successful shutdown of the service.

    // Or, if an error occurs during shutdown and RuntimeException is thrown from this method
    // the JBoss MSC container will log this incident to the system console for further analysis because JBoss MSC lifecycle stop() method can never fail.
}
```
### Asynchronous Stop

```
@Override
public void stop(StopContext context) {
    // Perform asynchronous cleanup tasks.
    CompletableFuture<Void> asyncCleanup = performAsyncCleanup();

    // Indicate that service is stopping and allow other services to continue to run.
    context.asynchronous();

    // Use a separate thread to complete or fail the service shutdown when asynchronous code is complete.
    asyncCleanup.whenCompleteAsync((result, throwable) -> {
        if (throwable != null) {
            // JBoss MSC lifecycle stop() method can never fail - so we need to log the incident to the system console
            Logger.log("stop method failed", throwable);
        } else {
            // Complete the service shutdown when cleanup is done.
            context.complete();
        }
    });
}
```

- The _context.asynchronous()_ method informs the container that the service is stopping but allows other services to continue running leveraging current JBoss MSC worker thread.
- Asynchronous cleanup tasks are performed using a _CompletableFuture_ or similar asynchronous mechanism.
- A separate thread or parallel execution is used to monitor the completion of asynchronous code.
- When the asynchronous code is complete, either exception is logged to the system console or _context.complete()_ is called accordingly.

## Conclusion

_StartContext_ and _StopContext_ in JBoss MSC provide powerful capabilities for managing the start-up and shutdown phases of your services.
Whether your services require _synchronous_ or _asynchronous_ initialization and cleanup, these contexts allow you to maintain control over
the service lifecycle. When using asynchronous methods, it's crucial to manage parallel execution to complete or fail the service installation
or shutdown as appropriate. By leveraging these contexts effectively, you can build robust and responsive services within the JBoss MSC container.

# Exploring ServiceController

[ServiceController](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceController.html) is a critical component in JBoss MSC 
that allows you to manage and monitor services within the container. It provides valuable insights into the status and dependencies of services,
enabling you to ensure the smooth operation of your application. In this chapter, we'll explore the possibilities that ServiceController offers.

## Monitoring Running Services

_ServiceController_ allows you to monitor the services that are currently running within the container.
User will retrieve the controller instance if and only if his service is successfully installed into the service container.
Here's an example of how to retrieve a _ServiceController_:

```
ServiceBuilder<?> sb = ... // configuring service
ServiceController<?> controller = sb.install();
```

Once you have the _ServiceController_, you can query its state and dependencies.

## Values

### Required Values

You can use the _requires()_ method to retrieve the set of dependency names that a service relies on. This is valuable for understanding the relationships between services in your application. For example:

```
Set<ServiceName> requiredValues = controller.requires();
```

### Provided Values

To access the set of value names that a service provides, you can use the _provides()_ method on the _ServiceController_. This is useful when you need to obtain an information about values the service provides. For example:

```
Set<ServiceName> providedValues = controller.provides();
```

### Missing Values

In scenarios where a service has unresolved dependencies, you can use _missing()_ method to identify the missing values. This method returns a set of unsatisfied dependencies that your service depends on. For example:

```
Set<ServiceName> missingValues = controller.missing();
```

## Service Mode and State

_ServiceController_ provides information about the mode and state of a service. These attributes give you insight into whether a service is actively running, starting, stopping, or has failed to start.

### Service Mode

The _getMode()_ method allows you to retrieve the mode of the service. Here's the example:

```
Mode mode = controller.getMode();
```

### Service State

The _getState()_ method provides the current state of the service. Here's the example:

```
State state = controller.getState();
```

## Conclusion

_ServiceController_ in JBoss MSC is a powerful tool for monitoring and managing services within the container.
With it, you can access required values, provided values, missing values, service mode, and service state.
This information is invaluable for ensuring that your services are running correctly and resolving any issues
that may arise during the application's lifecycle. By leveraging the capabilities of ServiceController,
you can build robust and reliable applications that take full advantage of the JBoss MSC container.
