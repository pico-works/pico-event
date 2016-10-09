package org.pico.event

import java.io.Closeable

import org.pico.disposal.SimpleDisposer
import org.pico.disposal.std.autoCloseable._

trait CompositeSinkSource[A, B] extends SinkSource[A, B] with SimpleDisposer {
  /** Publish an event to a sink.
    */
  override def publish(event: A): Unit = asSink.publish(event)

  /** Subscribe a subscriber to a source.  The subscriber will be invoked with any events that the
    * source may emit.
    */
  override def subscribe(subscriber: B => Unit): Closeable = asSource.subscribe(subscriber)
}

object CompositeSinkSource {
  def from[A, B](sink: Sink[A], source: Source[B]): SinkSource[A, B] = {
    new CompositeSinkSource[A, B] { self =>
      override val asSink: Sink[A] = self.disposes(sink)

      override val asSource: Source[B] = self.disposes(source)
    }
  }
}
