package org.pico.event

/** A SinkSource is both a Sink and a Source.
  * Any events published to the SinkSource will have a transformation function applied to it
  * before emitting the transformed event to subscribers.
  */
trait SinkSource[-A, +B] extends Sink[A] with Source[B]

object SinkSource {
  def apply[A, B](f: A => B): SinkSource[A, B] = SimpleSinkSource(f)

  def from[A, B](sink: Sink[A], source: Source[B]): SinkSource[A, B] = CompositeSinkSource.from(sink, source)
}
