package org.pico.event

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import org.pico.disposal.Disposer
import org.pico.disposal.std.autoCloseable._

trait Sink[-A] extends Disposer { self =>
  /** Get the Sink representation of this.
    */
  def asSink: Sink[A] = this

  /** Publish an event to a sink.
    */
  def publish(event: A): Unit

  /** Create a new Sink that applies a function to the event before propagating it to the
    * original sink.
    */
  def comap[B](f: B => A): Sink[B] = new Sink[B] { temp =>
    val selfRef = temp.swapDisposes(ClosedSinkSource, new AtomicReference(self))
    override def publish(event: B): Unit = selfRef.get().publish(f(event))
  }
}

object Sink {
  /** A sink that ignores values published to it.
    */
  val ignore = new Sink[Any] {
    override def publish(event: Any): Unit = ()
  }

  def apply[A](f: A => Unit): Sink[A] = {
    new Sink[A] {
      val active = new AtomicBoolean(true)

      this.onClose(active.set(false))

      override def publish(event: A): Unit = if (active.get()) f(event)
    }
  }
}
