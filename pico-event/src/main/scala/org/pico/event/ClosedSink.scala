package org.pico.event

import org.pico.disposal.ClosedDisposer

trait ClosedSink extends Sink[Any] with ClosedDisposer {
  override def publish(event: Any): Unit = ()

  override def comap[B](f: B => Any): Sink[B] = this
}

/** An already closed sink that will ignore any events published to it.
  */
object ClosedSink extends ClosedSink
