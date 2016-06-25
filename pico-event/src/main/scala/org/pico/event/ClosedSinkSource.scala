package org.pico.event

trait ClosedSinkSource extends SinkSource[Any, Nothing] with ClosedSink with ClosedSource

/** An already closed sink source that ignores any events published to it, never emits events and
  * never hold references to any subscribers it is given.
  */
object ClosedSinkSource extends ClosedSinkSource
