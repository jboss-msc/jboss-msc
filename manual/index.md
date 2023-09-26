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
- [ServiceBuilder.setInstance()](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceBuilder.html#setInstance(org.jboss.msc.Service)) to bypass the service instance to the service installer
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
