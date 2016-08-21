# Virtual Field Processor

An annotation processor for generating the delegate interface for the virtual
field pattern.


The pattern started to show itself on the JDK mailing lists around 2012 and
the advent of Java 8:
http://mail.openjdk.java.net/pipermail/lambda-dev/2012-August/005455.html


With the use of the Virtual Field Processor, the use of the pattern gets
simplified, and consists of an interface defined by the programmer and
annotated with '@VirtualField'. The pocessor then generates the child
interface that is responsible for the delegation through the default methods.
