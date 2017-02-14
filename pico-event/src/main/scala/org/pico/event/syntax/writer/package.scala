package org.pico.event.syntax

import java.io.Writer

import org.pico.event.Sink
import org.pico.event.io.NewlineCountWriter

package object writer {
  implicit class WriterOps_vYaV2CB(val self: Writer) extends AnyVal {
    /** Decorate a writer with the behaviour of counting newlines as they are written and publishing the counts
      * to the provided sink.
      */
    @inline final def countNewlinesIn(sink: Sink[Long]): Writer = NewlineCountWriter(self, sink)
  }
}
