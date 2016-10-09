package org.pico.event

import java.io.Closeable

import org.pico.disposal.{Closed, Disposer}

object ClosedSubscribers extends Subscribers[Any, Nothing] with Disposer {
  override def subscribe(subscriber: (Nothing) => Unit): Closeable = Closed

  override def publish(event: Any): Unit = ()

  override def houseKeep(): Unit = ()
}
