package org.pico.event

import java.io.Closeable

import org.pico.disposal.Disposer
import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._
import org.pico.event.syntax.hasForeach._

import scala.language.higherKinds
import org.pico.atomic.syntax.std.atomicReference._

trait Source[+A] extends Disposer { self =>
  /** Get the Source representation of this.
    */
  def asSource: Source[A] = this

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
  def mapConcat[F[_]: HasForeach, B](f: A => F[B]): Source[B] = {
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
  def foldRight[B](initial: B)(f: (A, => B) => B): View[B] = {
    val cell = Cell[B](initial)

    cell.disposes(this.subscribe(v => cell.update(a => f(v, a))))

    cell
  }

  /** Direct all events into the sink.
    */
  def into[B >: A](sink: Sink[B]): Closeable = self.subscribe(sink.publish)

  /** Create a new Source that emits only events that satisfies the predicate f
    *
    * @param f The predicate
    * @return New filtering source
    */
  def filter(f: A => Boolean): Source[A] = new SimpleBus[A] { temp =>
    temp += self.subscribe(a => if (f(a)) temp.publish(a))
  }

  /** Merge to sources such that events emitted from the left source will be emitted in the Left
    * case and events emitted from the right source will be emitted in the Right case.
    *
    * @param that The right source
    * @tparam B The type of the right source events
    * @return A new source that emits events from left and right sources.
    */
  def or[B](that: Source[B]): Source[Either[A, B]] = {
    val temp = Bus[Either[A, B]]
    temp += self.subscribe(e => temp.publish(Left(e)))
    temp += that.subscribe(e => temp.publish(Right(e)))
    temp
  }
}
