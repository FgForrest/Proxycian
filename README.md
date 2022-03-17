# Proxycian

Small Java library for generating dynamic proxies on top of [ByteBuddy](https://github.com/raphw/byte-buddy)
or [Javassist](https://github.com/jboss-javassist/javassist). You can generate data transfer objects, rich traits or
even whole implicit DAO implementations dynamically at runtime easily. This library solves the complex stuff, so you can
focus on application logic. Serializability, cloning are already solved by us. We also aim for transparent and easily
debuggable proxies, because as we know proxies is usually part of "magic" for the team. 
Hence, the name of this library - Proxycian as a magician for the proxies ;)

## History

Developers in [FG Forrest](https://www.fg.cz) use dynamic proxies successfully for over 10 years. Our initial 
implementation was based on [Spring framework](https://spring.io/) abstractions, but we quickly realized that
their implementation is overly complex and prone to subtle errors leading to memory leaks as well as poorly 
observable / debuggable. Our second implementation took advantage of [JBoss Javassist](https://www.javassist.org/)
but still kept many unnecessary abstractions and principles we learned from Spring implementation.

This implementation is third take on proxies that is the leanest and the most opinionated one so far. It was designed
from scratch based on current and actively developed libraries and with the emphasis on:

- simplicity
- clear and transparent class / method implementation caching
- debuggability - you just want your debugger to step in the dynamic implementation without much fuss around
- transparency - you need to easily find out why the library chose the implementation it chose

Let us know if we achieved our goals or not. Opinions and feedback is welcomed.

## What we do solve with Proxycian

Let's see a few practical examples what you can do with Proxycian:

### 1. stateful and dynamic traits

Java doesn't have multiple inheritance. But you can imitate it to certain level with default methods on interfaces,
but there will be always limitations. Class might implement multiple interfaces, but these interfaces can't have fields
and keep data in them. Also, you cannot decide which traits your class will have in runtime.

Proxycian allows us to create self-sustainable traits keeping both logic and data that don't require any orchestration
or cooperation with the main class they are attached to. Also, we can easily create specialized class with set of traits
selected in runtime, usually on some text-based configuration or user interaction with the application.

### 2. interception / delegation

We use Proxycian to wrap external classes - such as Spring [ReloadableResourceBundle](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/support/ReloadableResourceBundleMessageSource.html)
or [JDBC DataSource](https://docs.oracle.com/javase/7/docs/api/javax/sql/DataSource.html) to intercept calls in 
development environment to provide information for the developers - reporting which message codes / messages were
used while rendering the page, or which (and how many) SQL queries were executed through the DataSource.

There are many use-cases where the interception might come handy. You can make a class wrapping original and delegating
method calls to it by hand, but it quickly gets incomprehensible and maintaining is costly. AOP with dynamic proxies
offer more clever and shorter way to achieve the same.

### 3. mocking contracts

There are situations when your application works with some interface for which there is real implementation not yet
known, but eventually it'll be resolved. In our modular system the modules export and require some interfaces. In order
to fulfill them we needed to configure modules in order how they depended on each other. This was tricky for part of
our developers and soon there were situations that two modules might have needed each other (circular dependency).
This is of course sign of poorly architected modules, but the life brings situations when circular dependency might
represent the least of all evils.

Proxycian allows us to create dynamic proxy implementing required interface and use it for immediate wiring by 
the dependency injection mechanism. The module has its requirement fulfilled and can be started. Any call to the proxy
method will end with an exception, but we usually need to call method after entire system starts. As soon the other
module that provides the interface starts as well, the proxy internal state is filled with reference to 
the implementation and delegates each method call to it.

### 4. implicit DAO/Service interface implementation

Do you know [Ruby's Active Record](https://guides.rubyonrails.org/active_record_basics.html) or 
[Spring Data](https://spring.io/projects/spring-data) libraries? You can easily implement your own using Proxycian.
It's matter of a few lines of code.

## Prerequisites

- JDK 1.8 (currently, only JDK 8 is supported. We plan to support also JDK 11 and 17 in near future)
- Log4J 2 (2.17+)
- Apache Commons Langs (2.6+)
- ByteBuddy / Javassist are bundled in our library, there will be no conflict with possible existing libraries on your
  classpath in different version

## How to compile

Use standard Maven 3 command:

```
mvn clean install
```

## How to run tests

Run your tests in IDE or run:

```
mvn clean test
```

Help us maintain at least 80% code coverage!

## How to use

### How to integrate to your application

Include Proxycian library in your Maven descriptor (`pom.xml`):

``` xml
<dependency>   
    <groupId>one.edee.oss</groupId>   
    <artifactId>proxycian_bytebuddy</artifactId>   
    <version>1.0.2</version>
</dependency>
```

Or Gradle:

```
dependencies {
    compile 'one.edee.oss:proxycian_bytebuddy:1.0.2'
}
```

Or use `proxycian_javassist` if you prefer this implementation (it has much smaller memory JAR size). Otherwise,
ByteBuddy is preferred implementation because it's actively maintained and supports the newest JDK version.

### How to generate a dynamic proxy class

***Note:** In this documentation we stick to ByteBuddy implementation in examples, but you can easily translate all of them
to Javassist implementation by replacing word `ByteBuddy` with `Javassist`. The contracts are identical in Proxycian.*

To create new class with requested contract just use:

``` java
final Class<?> theInstance = ByteBuddyProxyGenerator.getProxyClass(
	Person.class,
	Trait1.class,
	Trait2.class
);
```

If you call it for the first time new class extending `Person.class` and implementing `Trait1.class` and `Trait2.class`
is created for you. If you call it second time, you'll receive previously created (cached) class with that contract.
The cache is kept in the static field of the generator and might be anytime cleared by 
calling `ByteBuddyProxyGenerator.clearClassCache()`.

If you want to extend some class it must be stated as the first class of the proxy contract, but you might also create
proxies based on a bunch of interfaces and no superclass (then the `java.lang.Object` becomes the superclass of the proxy).

You can also create proxies based on superclasses without default constructor (i.e. having only constructor with one or
more arguments). Imagine that the `Person.class` has the only constructor `protected Person(String firstName, String lastName)`:

``` java
final Class<?> theInstance = ByteBuddyProxyGenerator.getProxyClass(
	new Class<?>[]{
		Person.class,
		Trait1.class,
		Trait2.class
	},
	new Class<?>[]{
		String.class,
		String.class
	}
);
```

You can also specify a classloader that will maintain the created class, but this is usually not necessary. Proxycian
uses by default the same classloader that loads `ByteBuddyProxyGenerator.class` itself.

But this is not the way Proxycian was meant to be used - read the next chapter for general usage scenario.

### How to generate a dynamic proxy instance

Creating classes is not the common way how you'll create proxies. You usually want the instance of the proxy and not 
the class. You can achieve this in a single call when both class and first instance is created at once:

``` java
final Object theInstance = ByteBuddyProxyGenerator.instantiate(
	new ProxyRecipe(
		Person.class,
		new Class[] {Trait1.class, Trait2.class}, 
		new Advice[]{LocalDataStoreAdvice.INSTANCE}
	),
	new GenericBucket()
);
```

Proxycian uses abstraction of `ProxyRecipe` that is used to wrap definition of the contract (i.e. what interfaces will
the proxy have) as well as the logic that will handle calls to methods of the contract (interfaces). `Advice` is 
an abstraction for implementation logic and call filtering logic - i.e. which method calls will be serviced by this
particular Advice.

Second argument - in our case `GenericBucket` is the single object maintaining the state of the proxy. It's the state
you'll provide and control.

***Note:** It's recommended to cache the ProxyRecipe and not create it again and again with each call of `instantiate` 
method as you see in the example.*

#### Advice

Advice is **strictly** stateless and if you create new Advice we recommend defining no instance fields,
creating private non-args constructor and providing single public static field INSTANCE which provides access to the
advice instance (as you see in the example: `LocalDataStoreAdvice.INSTANCE`).

There are two different kind of Advices:

- regular `Advice`: the advice only defines the filters for methods it intercepts and the implementation of them,
  see example advice `one.edee.oss.proxycian.trait.beanMemoryStore.BeanMemoryStoreAdvice`
- `IntroductionAdvice`: is same as regular advice but also introduces new interface (or set of interfaces) to the proxy;
  this means that it's not necessary to state this interface in the ProxyRecipe explicitly, but it will be automatically
  added to the proxy contract whenever the IntroductionAdvice is part of the recipe

Each advice may require state object to implement certain contract so that they can keep necessary state in it (remember
Advices are stateless). If they don't work with state, they just require generic `Object.class` contract to be fulfilled
by the state object which matches everything.

The simple Advice may look like this:

``` java
public class ExampleAdvice implements Advice<Object> {
	private static final long serialVersionUID = 4100044042153442374L;

	@Override
	public Class<Object> getRequestedStateContract() {
		return Object.class;
	}

	@Override
	public List<MethodClassification<?, Object>> getMethodClassification() {
		return Collections.singletonList(
			new PredicateMethodClassification<>(
				/* description */   "Hello world method",
				
				/* matcher */       (method, proxyState) -> 
					"helloWorld".equals(method.getName()) && 
						String.class.equals(method.getReturnType()) && 
						method.getParameterCount() == 1 &&
						String.class.equals(method.getParameterTypes()[0]),
				
				/* methodContext */ MethodClassification.noContext(),
				
				/* invocation */    (proxy, method, args, methodContext, proxyState, invokeSuper) -> 
						"Hello world, " + args[0]
			)
		);
	}

}
```

This advice reacts to method call with following signature `String helloWorld(String myName);` and when calling
`proxy.helloWorld("Jan")` returns `Hello world, Jan` in response. Advice provides set of so called `MethodClassification`
that are used to intercept the proper methods on proxy interface. There are two types of method classifications:

##### PredicateMethodClassification

The classification consists of 4 parts:

**1. description** - simple string description of the classification, it's used only for developer orientation 
in debugging sessions, it has no other real usage in the Proxycian

**2. matcher** - represents simple predicate that accepts `java.lang.Method` and reference to the proxy state object
and returns TRUE if this method classifier intercepts this method call

**3. method context** - represents function that takes `java.lang.Method` and reference to the proxy state object and
returns DTO object that contains extracted information from the method signature that is necessary for the implementation
logic, the DTO is created only for the first call and cached so all additional method calls will reuse this method context.
It may therefore contain rather complex logic without fear of affecting proxy method call performance.

**4. method implementation** - the last piece of puzzle will provide the final logic for the method, this is the only
part executed everytime the proxy method is called

The method call interception logic is straight forward - when method on proxy is called for the first time we need to 
resolve the proper implementation. The Proxycian will iterate over all advices and within them over all the method 
classifiers the advice provides and select **the first** method classifier which predicate returns true. If predicates
of your advices overlap (the very same method might be intercepted and handled by Advice1 as well as Advice2) the Advice
which is defined first wins. The predicates may overlap even within single advice, so even the order in which you specify
method classifiers is crucial.

When method classifier is selected, function that creates method context is called and its result is cached into the
`ByteBuddyProxyGenerator` method cache for the key `one.edee.oss.proxycian.cache.ClassMethodCacheKey`. Finally,
the implementation part is executed. Next time the same method is called (maybe on other instance of the same proxy class)
the implementation with method context is quickly retrieved from the internal hash map and executed. With each call on
proxy instance you pay the price of single lookup to the hash map and delegating call to associated implementation object.

Method cache can be anytime reset by calling `ByteBuddyProxyGenerator.clearMethodClassificationCache()`. Your method
classification can also add custom data to the method cache key should it be necessary.

##### DirectMethodClassification

This implementation is similar to `PredicateMethodClassification` in principle. It just combines the predicate with
method context creation together. These two aspects of the method classification contract are some time very similar and
might be quite expensive. It makes sense to support this approach as well so that the same logic can be used for matching
the method as well as creating method context for it.

The `PredicateMethodClassification` in our example can be easily translated to `DirectMethodClassification` and vice versa:

``` java
new DirectMethodClassification<>(
	/* description */   "Hello world method",

	/* matcher */       (method, proxyState) -> {
		if ("helloWorld".equals(method.getName()) && String.class.equals(method.getReturnType()) &&
			method.getParameterCount() == 1 && String.class.equals(method.getParameterTypes()[0])) {
			return (proxy, theMethod, args, theProxyState, invokeSuper) -> "Hello world, " + args[0];
		} else {
			return null;
		}
	}
)
```

#### State object

State object is the target for all Java base methods such as `equals`, `hashCode`, `toString`, serialization and clone 
facility. We have single state object on purpose - it's much easier to track, debug and control data this way. The state 
object must fulfill the contract required by all the Advices of the proxy (do not confuse the contract required by the 
advices with the contract of the proxy itself!).