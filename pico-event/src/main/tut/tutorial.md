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

```tut:reset
import org.pico.event._

val bus = Bus[Int]
var count = 0
val subscription = bus.subscribe(e => count += e)
bus.publish(1)
assert(count == 1)
```

In the above code, a subscriber function `e => count += e` is registered on the bus.  When `1` is published
to the bus on the next line, the subscriber function is called, which adds one to the variable `count`.

## Sinks and Sources
The `Bus` is an object that implements two core abstractions of the `pico-event` library:  The `Sink` and
the `Source`.

The `Sink` supports the `publish` method and the `Source` supports the `subscribe` method.

This is part of the definition of `Sink`:

```scala
trait Sink[-A] extends Disposer { self =>
  def publish(event: A): Unit
  ...
}
```

A `Sink[A]` accepts events of type `A` via the `publish` method:

```tut:reset
import org.pico.event._

val sink: Sink[Int] = ClosedSink
sink.publish(42)
```

This is part of the definition of `Source`:

```scala
trait Source[+A] extends Disposer {
  def subscribe(subscriber: A => Unit): Closeable
  ...
}
```

A `Source[A]` emits events of type `A` to any subscribers.  A subscription is created by supplying
a subscriber to the source's `subscribe` method:
  
```tut:reset
import org.pico.event._

val source: Source[Int] = ClosedSource
val subscription = source.subscribe(println)
```

By creating a subscription as above, any events emitted by `source` will be printed.

## Subscription lifetimes
Subscriptions will continue to be active until either the subscription or source is closed or
disposed, or the subscription is garbage collected.  The subscription will maintain a reference to
the source so the source will not be garbage collected until the subscription garbage collector
or the subscription is disposed.

Both `Source` and `Sink` inherit from `Disposer`:

```scala
trait Disposer extends Closeable {
  def +=[D: Disposable](disposable: D): D = disposes(disposable)
  def disposes[D: Disposable](disposable: D): D
  def close(): Unit = disposables.getAndSet(Closed).dispose()
}
```

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

```tut:reset
import org.pico.event._

val stringSource: Source[String] = ClosedSource
val stringLengthSource: Source[Int] = stringSource.map(_.length)
```

## Effect method for Source

The `effect` method is similar to map.  It behaves much like `source.map(identity)`, i.e. it will
will produce a new source that emits the same events as the original - except that it will also
execute a side-effecting method before doing so.

```tut:reset
import org.pico.event._

val stringSource: Source[String] = ClosedSource
val printingStringSource: Source[String] = stringSource.effect(println)
```

## MapConcat method for Source
The `mapConcat` method is similar to `map`, except that the result type of the mapping function
is required to return an `Iterable`.  `mapConcat` will then produce a `Source` of the `Iterable`
element type that emits each element of the `Iterable`:

```tut:reset
import org.pico.event._
import org.pico.event.std.all._

val source1: Source[List[Int]] = ClosedSource
val source2: Source[Int] = source1.mapConcat(identity)
```

## Filter method for Source

The `Sink.filter` method can be used to derived a `Source` that only emits events that satisy a
predicate:

```tut:reset
import org.pico.event._
import org.pico.event.std.all._

val source1: Source[Int] = ClosedSource
val source2: Source[Int] = source1.filter(_ % 2 == 0)
```

## Comap method for Sink

In a similar fashion, `Sink` has a `comap` method that returns a new `Sink` that transforms events
that are published before with a mapping method.  Unlike `map`, which takes the mapping method
`A => B`, the `comap` method takes a mapping method `B => A`.  Because `B` is now an argument
type instead of a return type, a type-hint for the argument type must be provided with the `comap`
method as shown below:

```tut:reset
import org.pico.event._

val stringSink: Sink[Int] = ClosedSink
val stringLengthSink: Sink[String] = stringSink.comap[String](_.length)
```

## Or method for Source

The `or` method will merge to sources such that events emitted from the left source will be emitted
in the `Left` case of `Either` and events emitted from the right source will be emitted in the `Right`
case of either:

```tut:reset
import org.pico.event._

val source1: Source[Int] = ClosedSource
val source2: Source[String] = ClosedSource
val source3: Source[Either[Int, String]] = source1 or source2
```

## DivertLeft and DivertRight method for Source
A `Source` that has an `Either` element type can divert events that are on one side of the `Either`
into a `Sink`.

The following diverts the left case:

```tut:reset
import org.pico.event._
import org.pico.event.syntax.source._

val source1: Source[Either[Int, String]] = ClosedSource
val sink: Sink[Int] = ClosedSink
val source2: Source[String] = source1.divertLeft(sink)
```
The following diverts the right case:

```tut:reset
import org.pico.event._
import org.pico.event.syntax.source._

val source1: Source[Either[Int, String]] = ClosedSource
val sink: Sink[String] = ClosedSink
val source2: Source[Int] = source1.divertRight(sink)
```

## Observable Views
Observable views have the type `View[A]`.  They model values that change over time.

The simplest view that can be constructed is a view that never changes.  For example, the following
creates a view with the value 1:

```tut:reset
import org.pico.event._

val view1 = View(1)
assert(view1.value == 1)
```

A view that changes over time can be constructed from a source.  The simplest such view is `latest`,
which constructs a view that maintains a value equal to the latest value emitted by a stream.  It
is initialised with an initial value which serves as its value until the source emits a value.

```tut:reset
import org.pico.event._
import org.pico.event.syntax.source._

val bus = SinkSource[Int, Int](identity)
val view = bus.asSource.latest(0)
assert(view.value == 0)
bus.publish(2)
assert(view.value == 2)
```

## Creating obervable views by folding sources

The `latest` method is actually implemented in terms of another more generic `foldRight` method:

```scala
def latest(intial: A): View[A] = this.foldRight(initial)((v, _) => v)
```

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

```tut:reset
import org.pico.event._
import org.pico.event.syntax.source._

val bus = SinkSource[Int, Int](identity)
val eventCount: View[Long] = bus.eventCount
```

## Observable Cells
Observable variables have the type `Cell[A]`.  They can be created by calling the
companion object constructor method with an initial value.  The cell can then be
updated via its `value` field:

```tut:reset
import org.pico.event._

val counter = Cell[Long](0)
counter.value = 1
```

It has semantics much like the `AtomicReference` type in the standard Java library,
so you can perform the same kinds of atomic operations like the following:

```tut:reset
import org.pico.event._

val counter = Cell[Long](0)
counter.getAndSet(1)
counter.update(_ + 1)
val success = counter.compareAndSet(1, 2)
```

Moreover, it exposes an event source that emits the new value after every value
change.  The following code prints every change to `counter`:

```tut:reset
import org.pico.event._

val counter = Cell[Long](0)
val source: Source[Long] = counter.source
val subscription = source.subscribe(println)
```

A `Cell` is also a `View` and inherit all its methods.
