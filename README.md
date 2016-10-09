# Virtual Field Processor

An annotation processor for generating the delegate interface for the virtual
field pattern.


## Quickstart
1. Add the annotation processor to your classpath like any other annotation processor.
2. Annotate your interface with @VirtualField

```java
@VirtualField
public interface MyInterface {
    void doStuff();
    void doAnotherStuff();
}
```

The following interface will be generated for you:

```java
public interface VirtualMyInterface {
    MyInterface getMyInterface();
    @Override
    default void doStuff() {
        getMyInterface().doStuff();
    }
    @Override
    default void doAnotherStuff() {
        getMyInterface().doAnotherStuff();
    }
}
```

Now implement the first interface, and then you can seamlessly compose that implementation
into other classes with the generated interface.
No need to write boilerplate delegation code ever again!

```java
public class Printer implements MyInterface {
    @Override
    public void doStuff() {
        System.out.println("Doing some stuff");
    }
    @Override
    public void doAnotherStuff() {
        System.out.println("Doing another stuff");
    }
}
```

```java
public class Composer implements VirtualMyInterface {

    private MyInterface delegate = new Printer();

    @Override
    public MyInterface getMyInterface() {
        return delegate;
    }
}
```

Then use Composer just like you would any other implementation of MyInterface:

```java
MyInterface ohMy = new Composer();
ohMy.doStuff(); //prints 'Doing some stuff'
ohMy.doAnotherStuff(); // prints 'Doing another stuff'
```

## The virtual field pattern

With Java 8 default methods on interfaces got introduced. These methods are mainly
intended for API evolution purposes, but have some other advantages as well, for example
there are some trivial behaviour that can be achieved directly on the interface, by
defining it based on the other interface methods.

The virtual field pattern arose from recognizing that delegation is such a trivial
behaviour that can be achieved with default methods.

The pattern looks like the following:

Given an interface called 'Contract':

```java
public interface Contract {
    String methodA();
    void methodB(Date arg);
}
```

Given another class 'ContractImplementor' that implements the 'Contract':

```java
public class ContractImplementor implements Contract {
    @Override
    public String methodA() {...}
    @Override
    public void methodB(Date arg){...}
}
```

The virtual field pattern allows a third class to implement 'Contract' by delegation,
without having to implement the delegation logic over and over again.

It works the following way: have an interface that extends 'Contract' called 'VirtualContract':

```java
public interface VirtualContract extends Contract {
    Contract delegate();
    @Override
    default String methodA() {
        return delegate().methodA();
    }
    @Override
    default void methodB(Date arg){
        delegate().methodB(arg);
    }
}
```

Now if you have a class Delegator that wants to implement Contract by delegating calls to Contract
to ContractImplementor, it just has to implement a single method called delegate():

```java
public class Delegator implements VirtualContract {
    private Contract contract = new VirtualContract();

    Contract delegate() {
        return contract;
    }
}
```

The methodA and methodB methods are now directly available on a Delegator instance, and calls to
those methods are delegated to the contract field of the Delegator instance.

## History

The pattern started to show itself on the JDK mailing lists around 2012 and
the advent of Java 8:
http://mail.openjdk.java.net/pipermail/lambda-dev/2012-August/005455.html


With the use of the Virtual Field Processor, the use of the pattern gets
simplified, and consists of an interface defined by the programmer and
annotated with '@VirtualField'. The pocessor then generates the child
interface that is responsible for the delegation through the default methods.
