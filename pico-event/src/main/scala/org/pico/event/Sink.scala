package org.pico.event

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import org.pico.disposal.SimpleDisposer
import org.pico.disposal.std.autoCloseable._

trait Sink[-A] extends SimpleDisposer { self =>
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
  /** Create a sink that calls the side-effecting function for every event emitted.
    *
    * @param f The side-effecting function
    * @tparam A The type of the event
    * @return A sink that invokes the side-effecting function for every event emitted.
    */
  def apply[A](f: A => Unit): Sink[A] = {
    new Sink[A] {
      val active = new AtomicBoolean(true)

      this.onClose(active.set(false))

      override def publish(event: A): Unit = if (active.get()) f(event)
    }
  }
}
