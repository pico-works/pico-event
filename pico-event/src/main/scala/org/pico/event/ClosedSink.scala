package org.pico.event

/** An already closed sink that will ignore any events published to it.
  */
object ClosedSink extends Sink[Any] {
  override def publish(event: Any): Unit = ()
}
