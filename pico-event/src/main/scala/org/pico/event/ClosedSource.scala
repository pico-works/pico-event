package org.pico.event

import java.io.Closeable

import org.pico.disposal.{Closed, ClosedDisposer}
import org.pico.disposal.std.autoCloseable._

trait ClosedSource extends Source[Nothing] with ClosedDisposer {
  override def subscribe(subscriber: Nothing => Unit): Closeable = Closed

  override def map[B](f: Nothing => B): Source[B] = ClosedSource

  override def effect(f: Nothing => Unit): Source[Nothing] = ClosedSource

  override def mapConcat[F[_]: HasForeach, B](f: Nothing => F[B]): Source[B] = ClosedSource

  override def merge[B](that: Source[B]): Source[B] = that

  override def foldRight[B](initial: B)(f: (Nothing, => B) => B): View[B] = View(initial)

  override def into[B >: Nothing](sink: Sink[B]): Closeable = Closed

  override def filter(f: Nothing => Boolean): Source[Nothing] = ClosedSource

  override def or[B](that: Source[B]): Source[Either[Nothing, B]] = {
    val temp = Bus[Either[Nothing, B]]
    temp += that.subscribe(e => temp.publish(Right(e)))
    temp
  }
}

/** An already closed source that will never emit events or hold references to any subscribers it
  * is given.
  */
object ClosedSource extends ClosedSource
