# pico-event
[![CircleCI](https://circleci.com/gh/pico-works/pico-event/tree/develop.svg?style=svg)](https://circleci.com/gh/pico-works/pico-event/tree/develop)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/492233dcb0824733a7cb7b60468ae418)](https://www.codacy.com/app/newhoggy/pico-works-pico-event?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pico-works/pico-event&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/492233dcb0824733a7cb7b60468ae418)](https://www.codacy.com/app/newhoggy/pico-works-pico-event?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=pico-works/pico-event&amp;utm_campaign=Badge_Coverage)
[![Gitter chat](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/pico-works/general)

Support library for atomic operations.

## Getting started

Add this to your SBT project:

```
resolvers += "dl-john-ky-releases" at "http://dl.john-ky.io/maven/releases"

libraryDependencies += "org.pico" %%  "pico-event" % "0.2.1"
```

## Publish-subscribe pattern
This library implements the publish-subscribe pattern with thread-safety, type-safety and lifetime
management in its simplest form.  It relies on the [pico-disposal](https://github.com/pico-works/pico-disposal)
library for managing subscribe lifetimes.

Included in the library are all the types necessary for components to interact with each other
via the publish-subscribe pattern and a small number of useful combinators for building source
and sink pipelines.

## Buses
A `Bus` is an object that events can be published to.  When an event is published to it, it will also emit
events.

  val bus = Bus[Int]
  var count = 0
  val subscription = bus.subscribe(e => count += e)
  bus.publish(1)
  count must_=== 1

In the above code, a subscriber function `e => count += e` is registered on the bus.  When `1` is published
to the bus on the next line, the subscriber function is called, which adds one to the variable `count`.

## Sinks and Sources
The `Bus` is an object that implements two core abstractions of the `pico-event` library:  The `Sink` and
the `Source`.

The `Sink` supports the `publish` method and the `Source` supports the `subscribe` method.

This is part of the definition of `Sink`:

    trait Sink[-A] extends Disposer { self =>
      def publish(event: A): Unit
      ...
    }

A `Sink[A]` accepts events of type `A` via the `publish` method:

    val sink: Sink[Int] = ???
    sink.publish(42)

This is part of the definition of `Source`:

    trait Source[+A] extends Disposer {
      def subscribe(subscriber: A => Unit): Closeable
      ...
    }

A `Source[A]` emits events of type `A` to any subscribers.  A subscription is created by supplying
a subscriber to the source's `subscribe` method:
  
    val source: Source[Int] = ???
    val subscription = source.subscribe(println)

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

## Map method for Source

The `map` method can be used to create one `Source` out of another by supplying a mapping method.
Events emitted by the original `Source` will have the mapping method applied to it with the
result emitted by the new `Source`

    val stringSource: Source[String]
    val stringLengthSource: Source[Int] = stringSource.map(_.length)

## Effect method for Source

The `effect` method is similar to map.  It behaves much like `source.map(identity)`, i.e. it will
will produce a new source that emits the same events as the original - except that it will also
execute a side-effecting method before doing so.

    val stringSource: Source[String] = ???
    val printingStringSource: Source[String] = stringSource.effect(println)

## MapConcat method for Source
The `mapConcat` method is similar to `map`, except that the result type of the mapping function
is required to return an `Iterable`.  `mapConcat` will then produce a `Source` of the `Iterable`
element type that emits each element of the `Iterable`:

    val source1: Source[List[Int]] = ???
    val source2: Source[Int] = source1.mapConcat(identity)

## Filter method for Source

The `Sink.filter` method can be used to derived a `Source` that only emits events that satisy a
predicate:

    val source1: Source[Int]
    val source2: Source[Int] = source1.filter(_ % 2 == 0)

## Comap method for Sink

In a similar fashion, `Sink` has a `comap` method that returns a new `Sink` that transforms events
that are published before with a mapping method.  Unlike `map`, which takes the mapping method
`A => B`, the `comap` method takes a mapping method `B => A`.  Because `B` is now an argument
type instead of a return type, a type-hint for the argument type must be provided with the `comap`
method as shown below:

    val stringSink: Sink[Int]
    val stringLengthSink: Sink[String] = stringSink.comap[String](_.length)

## Or method for Source

The `or` method will merge to sources such that events emitted from the left source will be emitted
in the `Left` case of `Either` and events emitted from the right source will be emitted in the `Right`
case of either:

    val source1: Source[Int] = ???
    val source2: Source[String] = ???
    val source3: Source[Either[Int, String]] = source1 or source2

## DivertLeft and DivertRight method for Source
A `Source` that has an `Either` element type can divert events that are on one side of the `Either`
into a `Sink`.

The following diverts the left case:

    val source1: Source[Either[Int, String]] = ???
    val sink: Sink[Int] = ???
    val source2: Source[String] = source1.divertLeft(sink)

The following diverts the right case:

    val source1: Source[Either[Int, String]] = ???
    val sink: Sink[String] = ???
    val source2: Source[Int] = source1.divertRight(sink)

## Observable Views
Observable views have the type `View[A]`.  They model values that change over time.

The simplest view that can be constructed is a view that never changes.  For example, the following
creates a view with the value 1:

    val view1 = View(1)
    view1.value must_=== 1

A view that changes over time can be constructed from a source.  The simplest such view is `latest`,
which constructs a view that maintains a value equal to the latest value emitted by a stream.  It
is initialised with an initial value which serves as its value until the source emits a value.

    val bus =  Bus[Int]
    val view = bus.asSink.latest(0)
    view must_=== 0
    bus.publish(2)
    view must_=== 2

## Creating obervable views by folding sources

The `latest` method is actually implemented in terms of another more generic `foldRight` method:

    def latest(intial: A): View[A] = this.foldRight(initial)((v, _) => v)

The first argument of `foldRight` is the initial value of the resulting view.  The second argument
is a function that describes how to combined events with the current value of the view to produce
a new value for the view.

The implementation of `foldRight` uses optimistic lock-free atomic updates to the value so it is
thread-safe even in situations that the source from which the view is derived emits events in different
threads.

This form of update will attempt to apply the update function to modify the value optimistically and
atomically.  If the update discovers that the value has changed before it could apply the update it
will abort and retry, calling the update function again.  This means *it is unsafe to perform any
side-effects in the update function*.  Because of the reties, it is also sensible to ensure that
the update function is as fast as possible.

## EventCount method for Source

A `Source` has an `eventCount` method that returns a `View` that counts how many times the original
source has changed values since the `View` was created.

    val view: View[Int] = ???
    val eventCount: View[Long] = view.eventCount

## Observable Cells
Observable variables have the type `Cell[A]`.  They can be created by calling the
companion object constructor method with an initial value like this:

    val counter = Cell[Long](0)

And updated using the `value` property:

    counter.value = 1

It has semantics much like the `AtomicReference` type in the standard Java library,
so you can perform the same kinds of atomic operations like the following:

    counter.getAndSet(1)
    counter.update(_ + 1)
    val success = counter.compareAndSet(1, 2)

Moreover, it exposes an event source that emits the new value after every value
change.  The following code prints every change to `counter`:

    val source: Event[Long] = counter.source
    val subscription = source.subscribe(println)

A `Cell` is also a `View` and inherit all its methods.
