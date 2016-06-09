package org.pico.event

import java.io.Closeable

import org.pico.disposal.Closed

/** An already closed source that will never emit events or hold references to any subscribers it
  * is given.
  */
object ClosedSource extends Source[Nothing] {
  override def subscribe(subscriber: Nothing => Unit): Closeable = Closed
}
