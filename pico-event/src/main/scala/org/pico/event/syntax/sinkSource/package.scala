package org.pico.event.syntax

import org.pico.disposal.std.autoCloseable._
import org.pico.event.syntax.source._
import org.pico.event.{Bus, SinkSource, Source}

import scala.concurrent.{ExecutionContext, Future}

package object sinkSource {
  implicit class SinkSourceOps_iY4kPqc[A, B](val self: SinkSource[A, Future[B]]) extends AnyVal {
    /** Return a source which emits events whenever the futures from the original
      * source complete.  Success values will be emitted on the right and failures
      * will be emitted on the left.
      */
    @deprecated("Use completed instead")
    def scheduled(implicit ec: ExecutionContext): Source[Either[Throwable, B]] = completed

    /** Return a source which emits events whenever the futures from the original
      * source completes.  Success values will be emitted on the right and failures
      * will be emitted on the left.
      */
    def completed(implicit ec: ExecutionContext): Source[Either[Throwable, B]] = {
      val bus = Bus[Either[Throwable, B]]

      bus.disposes(self.asSource.scheduled into bus)

      bus
    }
  }
}
