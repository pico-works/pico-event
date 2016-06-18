package org.pico.event

import java.io.Closeable
import java.util.concurrent.atomic.AtomicReference

import org.pico.disposal.std.autoCloseable._

/** A SinkSource is both a Sink and a Source.
  * Any events published to the SinkSource will have a transformation function applied to it
  * before emitting the transformed event to subscribers.
  */
trait SinkSource[-A, +B] extends Sink[A] with Source[B] { self =>
  /** Create a new Source that will emit transformed events that have been emitted by the original
    * Source.  The transformation is described by the function argument.
    */
  final def dimap[C, D](f: C => A)(g: B => D): SinkSource[C, D] = new SinkSource[C, D] { temp =>
    val cSinkRef    = temp.swapDisposes(ClosedSinkSource, new AtomicReference(self.comap(f)))
    val dSourceRef  = temp.swapDisposes(ClosedSinkSource, new AtomicReference(self.map(g)))

    cSinkRef  .get().disposes(temp)
    dSourceRef.get().disposes(temp)

    override def subscribe(subscriber: D => Unit): Closeable = dSourceRef.get().subscribe(subscriber)
    override def publish(event: C): Unit = cSinkRef.get().publish(event)
  }
}

object SinkSource {
  def apply[A, B](f: A => B): SinkSource[A, B] = SimpleSinkSource(f)
}
