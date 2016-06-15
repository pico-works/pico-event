package org.pico.event

import java.io.Closeable

import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._
import org.pico.disposal.{Closed, Disposer}

trait Source[+A] extends Disposer { self =>
  /** Subscribe a subscriber to a source.  The subscriber will be invoked with any events that the
    * source may emit.
    */
  def subscribe(subscriber: A => Unit): Closeable

  /** Create a new Source that will emit transformed events that have been emitted by the original
    * Source.  The transformation is described by the function argument.
    */
  def map[B](f: A => B): Source[B] = {
    new SimpleSinkSource[A, B] { temp =>
      override def transform: A => B = f

      disposes(self.subscribe(temp.publish))
    }
  }

  /** Side effect to execute when an event occurs.
    *
    * @param f The side effecting function
    * @return a source that emits the same events after the side effect has been performed
    */
  def effect(f: A => Unit): Source[A] = self.map { a => f(a); a }

  /** From a function that maps each event into an iterable event, create a new Source that will
    * emit each element of the iterable event.
    */
  def mapConcat[B](f: A => Iterable[B]): Source[B] = {
    new SimpleBus[B] { temp =>
      temp += self.subscribe(f(_).foreach(temp.publish))
    }
  }

  /** Compose two sources of compatible type together into a new source that emits the same events
    * as either of the two originals.
    */
  def merge[B >: A](that: Source[B]): Source[B] = {
    new SimpleBus[B] { temp =>
      temp += self.subscribe(temp.publish) :+: that.subscribe(temp.publish)
    }
  }

  /** Fold the event source into a value given the value's initial state.
    *
    * @param f The folding function
    * @param initial The initial state
    * @tparam B Type of the new value
    * @return The value.
    */
  def foldRight[B](initial: B)(f: (A, => B) => B): Live[B] = Live.foldRight(self)(initial)(f)

  /** Direct all events into the sink.
    */
  def into(sink: Sink[A]): Closeable = self.subscribe(sink.publish)

  /** Create a new Source that emits only events that satisfies the predicate f
    *
    * @param f The predicate
    * @return New filtering source
    */
  def filter(f: A => Boolean): Source[A] = new SimpleBus[A] { temp =>
    temp += self.subscribe(a => if (f(a)) temp.publish(a))
  }

  def or[B](that: Source[B]): Source[Either[A, B]] = {
    val temp = Bus[Either[A, B]]
    temp += self.subscribe(e => temp.publish(Left(e)))
    temp += that.subscribe(e => temp.publish(Right(e)))
    temp
  }
}

object Source {
  /** A source that never emits any events.
    */
  val empty = new Source[Nothing] {
    override def subscribe(subscriber: Nothing => Unit): Closeable = Closed
  }
}
