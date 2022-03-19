# Proxycian

A small Java library for generating dynamic proxies on top of [ByteBuddy](https://github.com/raphw/byte-buddy)
or [Javassist](https://github.com/jboss-javassist/javassist). You can generate data transfer objects, rich traits or
even whole implicit DAO implementations dynamically at runtime easily. This library solves the complex stuff, so you can
focus on application logic. Serializability and cloning are already solved by us. We also aim for transparent and easily
debuggable proxies, because as we know proxies is usually part of "magic" for the team. 
Hence, the name of this library - Proxycian as a magician for the proxies ;)

## History

Developers in [FG Forrest](https://www.fg.cz) use dynamic proxies successfully for over 10 years. Our initial 
implementation was based on the [Spring framework](https://spring.io/) abstractions, but we quickly realized that
their implementation is overly complex and prone to subtle errors leading to memory leaks as well as being poorly 
observable / debuggable. Our second implementation took advantage of [JBoss Javassist](https://www.javassist.org/)
but it still kept many unnecessary abstractions and principles we learned from the Spring implementation.

This implementation is our third take on proxies that is the leanest and the most opinionated one so far. It was designed
from scratch and based on current and actively developed libraries and with the emphasis on:

- simplicity
- clear and transparent classes / method implementation caching
- debuggability - you just want your debugger to step in the dynamic implementation without much fuss around
- transparency - you can easily find out why the library chose the implementation it chose

Let us know if we achieved our goals or not. Opinions and feedback is welcome.

## What we do solve with Proxycian

Let's see a few practical examples on what you can do with Proxycian:

### 1. stateful and dynamic traits

Java doesn't have multiple inheritance, but you can imitate it to certain level with default methods on interfaces,
but there will be always limitations. Classes might implement multiple interfaces, but these interfaces can't have fields
and keep data in them. Also, you cannot decide which traits your class will have in runtime.

Proxycian allows us to create self-sustainable traits keeping both logic and data that don't require any orchestration
or cooperation with the main class which they are attached to. Also, we can easily create specialized class with set of traits
selected in runtime, usually on some text-based configuration or user interaction with the application.

### 2. interception / delegation

We use Proxycian to wrap external classes - such as Spring [ReloadableResourceBundle](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/context/support/ReloadableResourceBundleMessageSource.html)
or [JDBC DataSource](https://docs.oracle.com/javase/7/docs/api/javax/sql/DataSource.html) to intercept calls in the 
development environment to provide information for the developers - reporting which message codes / messages were
used while rendering the page, or which (and how many) SQL queries were executed through the DataSource.

There are many use-cases where the interception might come handy. You can make a class wrapping original and delegate
method calls to it by hand, but it quickly gets incomprehensible and maintaining is costly. AOP with dynamic proxies
offer a more clever and shorter way to achieve the same.

### 3. mocking contracts

There are situations when your application works with some interface for which the real implementation is not yet
known, but eventually it will be resolved. In our modular system the modules export and require some interfaces. In order
to fulfill them we needed to configure modules, how they depended on each other. This was the tricky for part of
our developers and soon there were situations when two modules might have needed each other (circular dependency).
This is a sign of poorly architected modules, but life brings situations when a circular dependency might
resemble the least of all evils.

Proxycian allows us to create a dynamic proxy, which implements required interfaces and use it for immediate wiring by 
the dependency injection mechanism. The module has its requirement fulfilled and can be started. Any call to the proxy
method will end with an exception, but we usually need to call method after entire system starts. As soon the other
module that provides the interface starts, the proxy internal state is filled with a reference to 
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

Run your tests in an IDE or run:

```
mvn clean test
```

Help us maintain at least 80% code coverage!

## How to use

### How to integrate to your application

Include the Proxycian library in your Maven descriptor (`pom.xml`):

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

To create new a class with the requested contracts, just use:

``` java
final Class<?> theInstance = ByteBuddyProxyGenerator.getProxyClass(
	Person.class,
	Trait1.class,
	Trait2.class
);
```

If you call it for the first time, a new class extending `Person.class` and implementing `Trait1.class` and `Trait2.class`
is created for you. If you call it second time, you'll receive the previously created (cached) class with that contract.
The cache is kept in the static field of the generator and might be anytime cleared by 
calling `ByteBuddyProxyGenerator.clearClassCache()`.

If you want to extend some class, it must be stated as the first class of the proxy contract, but you might also create
proxies based on a bunch of interfaces and no superclass (then the `java.lang.Object` becomes the superclass of the proxy).

You can also create proxies based on superclasses without a default constructor (i.e. having only one constructor with one or
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

But this is not the way Proxycian was meant to be used - read the next chapter for a general usage scenario.

### How to generate a dynamic proxy instance

Creating classes is not the common way how you'll create proxies. You usually want the instance of the proxy and not 
the class. You can achieve this in a single call, when both classes and a first instance is created at once:

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

Proxycian uses an abstraction of `ProxyRecipe` that is used to wrap the definition of the contract (i.e. what interfaces will
the proxy have) as well as the logic that will handle the calls to methods of the contract (interfaces). `Advice` is 
an abstraction for implementation of the logic and call filtering logic - i.e. which method calls will be serviced by this
particular Advice.

Second argument - in our case `GenericBucket` is a single object maintaining the state of the proxy. It's the state
you'll provide and control.

***Note:** It's recommended to cache the ProxyRecipe and not create it again and again with each call of `instantiate` 
method as you see in the example.*

#### Advice

Advice is **strictly** stateless and if you create a new Advice, we recommend defining the no instance fields,
creating private non-args constructor and providing a single public static field INSTANCE, which provides access to the
advice instance (as you see in the example: `LocalDataStoreAdvice.INSTANCE`).

There are two different kind of Advices:

- regular `Advice`: the advice only defines the filters for methods it intercepts, and the implementation of them;
  see example advice `one.edee.oss.proxycian.trait.beanMemoryStore.BeanMemoryStoreAdvice`
- `IntroductionAdvice`: is the same as regular advice but also introduces a new interface (or set of interfaces) to the proxy;
  this means that it is not necessary to state this interface in the ProxyRecipe explicitly, but it will be automatically
  added to the proxy contract whenever the IntroductionAdvice is part of the recipe

Each advice may require a state object to implement certain contracts, so that they can keep the necessary state in it (remember, 
Advices are stateless). If they don't work with the state, they just require generic `Object.class` contract to be fulfilled
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

This advice reacts to a method call with following the signature `String helloWorld(String myName);` and when calling
`proxy.helloWorld("Jan")` returns `Hello world, Jan` in response. Advice provides a set of so called `MethodClassification`
that are used to intercept the proper methods on a proxy interface. There are two types of method classifications:

##### PredicateMethodClassification

The classification consists of 4 parts:

**1. description** - a simple string description of the classification, it's used only for developer orientation 
in debugging sessions, it has no other real usage in the Proxycian.

**2. matcher** - represents a simple predicate that accepts `java.lang.Method` and a reference to the proxy state object,
and returns TRUE if this method classifier intercepts this method call.

***Note:** you can use static helper methods in `one.edee.oss.proxycian.util.ReflectionUtils` interface in your predicate.
For example if you want to check whether the called method is the same method in particular interface, you can use this
expression: `ReflectionUtils.isMethodDeclaredOn(method, LocalDataStore.class, "getLocalData", String.class)` where
`method` is the called method, `LocalDataStore.class` is the checked interface, `"getLocalData"` is the name of the
method in the interface and `String.class` is the single method parameter. You can find more handy methods here as well.*

**3. method context** - it represents function that takes `java.lang.Method` and references it to the proxy state object and
returns a DTO object that contains extracted information from the method signature that is necessary for the implementation
logic. The DTO is created only for the first call and cached so all additional method calls will reuse this method context.
It may therefore contain rather complex logic without fear of affecting proxy method call performance.

**4. method implementation** - the last piece of puzzle will provide the final logic for the method. This is the only
part executed everytime the proxy method is called

***Note:** if you want to invoke the original method (for example you want only to do something before / after the original
method executes), use expression: `return invokeSuper.call()`*

The method call interception logic is straightforward - when a method on a proxy is called for the first time, we need to 
resolve the proper implementation. The Proxycian will iterate over all advices and within them, over all the method 
classifiers, the advice provides and selects **the first** method classifier, which predicate returns true. If predicates
of your advices overlap (the very same method might be intercepted and handled by Advice1 as well as Advice2), the Advice
which is defined first wins. The predicates may overlap even within single advice, so even the order in which you specify
method classifiers is crucial. There are also "system methods" that are automatically handled by the Proxycian and these
have their own priority. The ordering of the method classifier is as follows:

1. `Object#hashCode()`
2. `Object#equals()`
3. `Object#toString()`
4. `Object#clone()`
5. `ProxyStateAccessor#getProxyState()`
6. all your method classifiers
7. when method is still not classified, the original method will be invoked - if it is "abstract", the call will fail

When method classifier is selected, a function that creates method context is called and it's result is cached into the
`ByteBuddyProxyGenerator` method cache for the key `one.edee.oss.proxycian.cache.ClassMethodCacheKey`. Finally,
the implementation part is executed. Next time the same method is called (maybe on another instance of the same proxy class),
the implementation with method context is quickly retrieved from the internal hash map and executed. With each call on the
proxy instance, you pay the price of a single lookup to the hash map and delegating a call to an associated implementation object.

Method cache can be reset at any time by calling `ByteBuddyProxyGenerator.clearMethodClassificationCache()`. Your method
classification can also add custom data to the method cache key, should it be necessary.

##### DirectMethodClassification

This implementation is similar to `PredicateMethodClassification` in it's principle. It just combines the predicate with the 
method context creation together. These two aspects of the method classification contract are some time very similar and
might be quite expensive. It makes sense to support this approach as well, so that the same logic can be used for matching
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

So if your method signature analysis is complex, and you would need to do the same operations both in predicate and
the execution function - use `DirectMethodClassification` otherwise stick to the `PredicateMethodClassification`.

#### State object

State object is the target for all Java base methods, such as `equals`, `hashCode`, `toString`and the serialization and clone 
facility. We have single state objects on purpose - it's much easier to track, debug and control data this way. The state of the
object must fulfill the contract required by all the Advices of the proxy (do not confuse the contract required by the 
advices with the contract of the proxy itself!). The state object lives with the proxy instance and gets garbage 
collected with it.

There is no required interface for the state object if you create NON-serializable instance of the class. However, if
you need an instance, that can be serialized using default Java serialization facility, you must implement 
`ProxyStateWithConstructorArgs` interface. The state itself must be Serializable and must allow keeping the original
constructor arguments used for instance creation, so that they can be reused in deserialization phase.

### Instantiation callback

There are certain use-cases when you have to "prepare" the instance immediately after creation, even before the method
classification logic gets in the way. For such case there is `one.edee.oss.proxycian.OnInstantiationCallback`. With it, you can 
implement and pass to the instantiation method. In this callback you can freely invoke method of the instance and no
dynamic logic stated in advices will be executed. Of course, calling abstract methods will trigger an exception.

## Prepared traits ready to use

### BeanMemoryStoreAdvice

This advice will intercept all method calls that follow [Java Beans](https://en.wikipedia.org/wiki/JavaBeans) contract
and stores the date into internal `HashMap` in the proxy state. This map can be accessed via. expression:
`((BeanMemoryStore)((ProxyStateAccessor)instance).getProxyState()).getLocalDataStoreIfPresent()`. In addition to 
standard Java Beans contracts, there is support for adding and removing 1:N items one by one. See example interface:

```java
public interface JavaBeanWithMultipleItems {
	List<String> getItems();
	void setItems(List<String> items);
	boolean addItem(String item);
	boolean removeItem(String item);
}
```

The behaviour describes following test case:

```java
@Test
public void shouldProxyJavaBeanWithMultipleItems() {
	final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
		new ProxyRecipe(
			new Class[] {JavaBeanWithMultipleItems.class},
			new Advice[] {BeanMemoryStoreAdvice.ALL_METHOD_INSTANCE}
		),
		new GenericBucket()
	);

	assertTrue(theInstance instanceof JavaBeanWithMultipleItems);
	final JavaBeanWithMultipleItems proxy = (JavaBeanWithMultipleItems) theInstance;

	proxy.addItem("A");
	proxy.addItem("B");
	proxy.addItem("C");

	assertArrayEquals(new String[] {"A", "B", "C"}, proxy.getItems().toArray(new String[0]));

	proxy.removeItem("B");

	assertArrayEquals(new String[] {"A", "C"}, proxy.getItems().toArray(new String[0]));
}
```

### LocalDataStoreAdvice

This is your handy advice that allows you to store any data from execution functions, or even default methods of 
your interfaces. See contract of `LocalDataStore` interface for more information.

If you add this advice to your dynamic proxy, you can then define multiple other "traits" that are merely interfaces
with default methods that can take advantage of the internal memory store to become "stateful". See following example:

```java
public interface ExpensiveComputer extends LocalDataStore {

  default double computePi() {
    return computeLocalDataIfAbsent("cachedPi", () -> {
      double pi = 0;
      for (int i = 1; i < 1_000_000; i++) {
        pi += Math.pow(-1, i+1) / (2 * i - 1);
      }
      return 4 * pi;
    });
  }

}
```

You can add `ExpensiveComputed` to any dynamic proxy, now having `LocalDataStoreAdvice`, and you will have an object that 
will return a computed PI to 1 mil. iterations. As you can see the expensive computation will happen only once on that
instance of dynamic proxy, because next time you call that method you'll receive memoized value from the first call.

### DelegateCallsAdvice

This advice lets you delegate calls to all methods of single interface directly to the state object or the object that
is reachable from the state object. This allows you to compose multiple interface delegations at once. Beware, this
example is quite long (to shorten it a little bit we use [Lombok](https://projectlombok.org/) annotations in it):

```java
public interface NameInterface {

	String getFullName();

	String getFirstName();
	String getLastName();

	void setFirstName(String firstName);
	void setLastName(String lastName);

}

@Data
public static class NameImplementation implements Serializable, NameInterface {
	private static final long serialVersionUID = -3190496823269251991L;
	private String firstName;
	private String lastName;

	public String getFullName() {
		return firstName + " " + lastName;
	}

}

public interface AgeInterface {

	int getAge();
	void setAge(int ageInYears);

}

@Data
public static class AgeImplementation implements Serializable, AgeInterface {
	private static final long serialVersionUID = -2520784598151746890L;
	private int age;
}

public interface PersonInterface {

	String getPersonDescription();

}

@Data
public static class CompositionState implements Serializable, PersonInterface {
	private static final long serialVersionUID = 3427985599976732264L;
	private final NameImplementation nameHolder = new NameImplementation();
	private final AgeImplementation ageHolder = new AgeImplementation();

	@Override
	public String getPersonDescription() {
		return nameHolder.getFullName() + " of age " + ageHolder.getAge();
	}
}

  @Test
  public void ByteBuddyProxyRecipeGenerator_DelegateCallsOnSubProperty() {
    final Object theInstance = ByteBuddyProxyGenerator.instantiateSerializable(
            new ProxyRecipe(
                    DelegateCallsAdvice.getInstance(NameInterface.class, o -> ((CompositionState)o).getNameHolder()),
                    DelegateCallsAdvice.getInstance(AgeInterface.class, o -> ((CompositionState)o).getAgeHolder()),
                    DelegateCallsAdvice.getInstance(PersonInterface.class)
            ),
            new CompositionState()
    );

    assertTrue(theInstance instanceof NameInterface);
    final NameInterface nameProxyContract = (NameInterface) theInstance;
    nameProxyContract.setFirstName("Jan");
    nameProxyContract.setLastName("Novotný");

    assertTrue(theInstance instanceof AgeInterface);
    final AgeInterface ageProxyContract = (AgeInterface) theInstance;
    ageProxyContract.setAge(43);

    assertTrue(theInstance instanceof PersonInterface);
    assertEquals("Jan Novotný of age 43", ((PersonInterface)theInstance).getPersonDescription());
  }
```
