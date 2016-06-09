package org.pico.event

import java.io.Closeable

import org.pico.disposal.Closed

/** An already closed sink source that ignores any events published to it, never emits events and
  * never hold references to any subscribers it is given.
  */
object ClosedSinkSource extends SinkSource[Any, Nothing] {
  override def subscribe(subscriber: (Nothing) => Unit): Closeable = Closed
  override def publish(event: Any): Unit = ()
}
