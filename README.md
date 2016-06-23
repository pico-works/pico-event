# pico-event
[![CircleCI](https://circleci.com/gh/pico-works/pico-event/tree/develop.svg?style=svg)](https://circleci.com/gh/pico-works/pico-event/tree/develop)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/492233dcb0824733a7cb7b60468ae418)](https://www.codacy.com/app/newhoggy/pico-works-pico-event?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pico-works/pico-event&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/492233dcb0824733a7cb7b60468ae418)](https://www.codacy.com/app/newhoggy/pico-works-pico-event?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pico-works/pico-event&amp;utm_campaign=Badge_Coverage)

Support library for atomic operations.

## Getting started

Add this to your SBT project:

```
resolvers += "dl-john-ky-releases" at "http://dl.john-ky.io/maven/releases"

libraryDependencies += "org.pico" %%  "pico-event" % "0.0.1-2"
```

## Publish-subscribe pattern
This library implements the publish-subscribe pattern with thread-safety, type-safety and lifetime
management in its simplest form.  It relies on the [pico-disposal](https://github.com/pico-works/pico-disposal)
library for managing subscribe lifetimes.

Included in the library are all the types necessary for components to interact with each other
via the publish-subscribe pattern and a small number of useful combinators for building source
and sink pipelines.

At the core of the library are two classes `Sink` and `Source`.

This is the interface of `Sink`:

    trait Sink[-A] extends Disposer { self =>
      def publish(event: A): Unit
      def comap[B](f: B => A): Sink[B]
    }

A `Sink[A]` accepts events of type `A` via the `publish` method:

    val sink: Sink[Int] = ???
    sink.publish(42)

By providing a mapping function to the `comap` method, it is possible to expose `Sink[A]` as a
sink of a different type `Sink[B]`

    val sink: Sink[Int] = ???
    val stringSink: Sink[String] = sink.comap(_.length)
    stringSink.publish("some string")

This is the interface of `Source`:

    trait Source[+A] extends Disposer {
      def subscribe(subscriber: A => Unit): Closeable
      def map[B](f: A => B): Source[B]
    }

A `Source[A]` emits events of type `A` to any subscribers.  A subscription is created by supplying
a subscriber to the source's `subscribe` method:
  
    val source: Source[Int] = ???
    val subscription = source.subscribe(e => println(e))

By creating a subscription as above, any events emitted by `source` will be printed.

## Subscription lifetimes
Subscriptions will continue to be active until either the subscription or source is closed or
disposed, or the subscription is garbage collected.  The subscription will maintain a reference to
the source so the source will not be garbage collected until the subscription garbage collector
or the subscription is disposed.

Both `Source` and `Sink` inherit from `Disposer`.

    trait Disposer extends Closeable {
      def +=[D: Disposable](disposable: D): D = disposes(disposable)
      def disposes[D: Disposable](disposable: D): D
      def close(): Unit = disposables.getAndSet(Closed).dispose()
    }

To dispose a `Source` means all subscriptions listening on it for events will be disposed and no
events will be emitted.

To dispose a `Sink` means that all calls to `publish` will be ignored.

The same applies to any type in this library that implements `Closeable`.  If they own
subscriptions, then closing those objects will also close those subscriptions.  If they maintain
references to resources, then closing those objects will also release those references to
facilitate garbage collection.

## Map function for Source

The map function can be used to create one `Source` out of another by supplying a mapping function.
Events emitted by the original `Source` will have the mapping function applied to it with the
result emitted by the new `Source`

    val stringSource: Source[String]
    val stringLengthSource: Source[Int] = stringSource.map(_.length)

## Effect function for Source

The effect function is similar to map.  It behaves much like `source.map(identity)`, i.e. it will
will produce a new source that emits the same events as the original - except that it will also
execute a side-effecting function before doing so.

    val stringSource: Source[String]
    val printingStringSource: Source[String] = stringSource.effect(println)

## Comap function for Sink

In a similar fashion, `Sink` has a `comap` function that returns a new `Sink` that transforms events
that are published before with a mapping function.  Unlike `map`, which takes the mapping function
`A => B`, the `comap` function takes a mapping function `B => A`.  Because `B` is now an argument
type instead of a return type, a type-hint for the argument type must be provided with the `comap`
method as shown below:

    val stringSink: Sink[Int]
    val stringLengthSink: Sink[String] = stringSink.comap[Int](_.length)

## Observable Variables
Observable variables have the type `Var[A]`.  They can be created by calling the
companion object constructor method with an initial value like this:

    val counter = Var[Long](0)

And updated using the `value` property:

    counter.value = 1

It has semantics much like the `AtomicReference` type in the standard Java library,
so you can perform the same kinds of atomic operations like the following:

    counter.getAndSet(1)
    counter.update(_ + 1)
    val success = counter.compareAndSet(1, 2)

Moreover, it exposes an event source that emits the new value after every value
change:

    val source: Event[Long] = counter.source
