package org.pico.event

import java.io.Closeable

import org.pico.disposal.Closed
import org.pico.disposal.std.autoCloseable._

/** An already closed source that will never emit events or hold references to any subscribers it
  * is given.
  */
object ClosedSource extends Source[Nothing] {
  override def subscribe(subscriber: Nothing => Unit): Closeable = Closed

  override def map[B](f: Nothing => B): Source[B] = ClosedSource

  override def effect(f: Nothing => Unit): Source[Nothing] = ClosedSource

  override def mapConcat[B](f: Nothing => Iterable[B]): Source[B] = ClosedSource

  override def merge[B](that: Source[B]): Source[B] = that

  override def foldRight[B](initial: B)(f: (Nothing, => B) => B): Live[B] = Live(initial)

  override def into(sink: Sink[Nothing]): Closeable = Closed

  override def filter(f: Nothing => Boolean): Source[Nothing] = ClosedSource

  override def or[B](that: Source[B]): Source[Either[Nothing, B]] = {
    val temp = Bus[Either[Nothing, B]]
    temp += that.subscribe(e => temp.publish(Right(e)))
    temp
  }
}
