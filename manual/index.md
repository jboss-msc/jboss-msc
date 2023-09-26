# JBoss Modular Service Container

## 1. About

**JBoss Modular Service Container** (JBoss MSC) is a lightweight highly concurrent  
dependency injection container for [Java](https://www.oracle.com/java/)  
It is developed under the [LGPL 2.1](../LICENSE.txt) license.  
The latest API documentation is available [here](../apidocs/index.html).

## 2. Basic Concepts

**JBoss MSC** is built on three key abstractions.

### 2.1 Service

[Service](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/Service.html) must be installed into
[Service Container](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceContainer.html) to become functional.  
Every service has two lifecycle phases - _start phase_ and _stop phase_.  
Services can start and stop either _synchronously_ or _asynchronously_.  
Furthermore every service has:  

- _dependencies_ - service may require values provided by other services
- _dependents_ - service may provide values to other services
- _mode_ - defines service's starting policy
- _state_ - current state of the service

## 2.2 Service Controller

Every [Service](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/Service.html) successfully installed
into [Service Container](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceContainer.html)
has associated [Service Controller](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceController.html) with it.  
Service controllers are useful for adding / removing [Lifecycle Listener](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/LifecycleListener.html)s to / from service at runtime,  
they can be used to change service mode at runtime (this includes also scheduling service for removal)  or to retrieve some useful  
information about service such as _provided values_, _required values_, _mode_, _state_ and so on.

## 2.3 Service Container

Every successfully installed service is stored in [Service Container](http://jboss-msc.github.io/jboss-msc/apidocs/org/jboss/msc/service/ServiceContainer.html).  
Service container provides information about available services and provides accessors  
for retrieving service controllers associated with installed services.  
Service container also provides _shutdown_ mechanism to stop it.
