# Proxycian

Small Java library for generating dynamic proxies on top of [ByteBuddy](https://github.com/raphw/byte-buddy)
or [Javassist](https://github.com/jboss-javassist/javassist). You can generate data transfer objects, rich traits or
even whole implicit DAO implementations dynamically at runtime easily. This library solves the complex stuff, so you can
focus on application logic. Serializability, cloning are already solved by us. We also aim for transparent and easily
debuggable proxies, because as we know proxies is usually part of "magic" for the team. Hence, the name of this library

- Proxycian as a magician for the proxies ;)

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

TOBEDONE