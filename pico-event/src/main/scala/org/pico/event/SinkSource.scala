package org.pico.event

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

import org.pico.disposal.std.autoCloseable._

/** A SinkSource is both a Sink and a Source.
  * Any events published to the SinkSource will have a transformation function applied to it
  * before emitting the transformed event to subscribers.
  */
trait SinkSource[-A, +B] extends Sink[A] with Source[B] { self =>
  /** Create a new Sink that applies a function to the event before propagating it to the
    * original sink.
    */
  override def comap[C](f: C => A): Sink[C] = new SinkSource[C, B] { temp =>
    val thatRef = temp.swapDisposes(ClosedSinkSource, new AtomicReference(SinkSource[C, A](f)))
    temp.disposes(thatRef.get().subscribe(self.publish))
    override def subscribe(subscriber: B => Unit): Closeable = temp.subscribe(subscriber)
    override def publish(event: C): Unit = thatRef.get().publish(event)
  }

  /** Create a new Source that will emit transformed events that have been emitted by the original
    * Source.  The transformation is described by the function argument.
    */
  override def map[C](f: B => C): SinkSource[A, C] = new SinkSource[A, C] { temp =>
    val thatRef = temp.swapDisposes(ClosedSinkSource, new AtomicReference(SinkSource[B, C](f)))
    val selfRef = temp.swapReleases(ClosedSinkSource, new AtomicReference(self))
    temp.disposes(self.subscribe(thatRef.get().publish))
    override def subscribe(subscriber: C => Unit): Closeable = thatRef.get().subscribe(subscriber)
    override def publish(event: A): Unit = selfRef.get().publish(event)
  }
}

object SinkSource {
  def apply[A, B](f: A => B): SinkSource[A, B] = new SimpleSinkSource[A, B] {
    override val transform = f
  }
}
