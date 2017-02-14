package org.pico.event.syntax

import java.io.OutputStream

import org.pico.event.Sink
import org.pico.event.io.ByteCountOutputStream

package object outputStream {
  implicit class OutputStreamOps_3hXLiwd(val self: OutputStream) extends AnyVal {
    /** Decorate an output stream with the behaviour of counting bytes as they are written and publishing the counts
      * to the provided sink.
      */
    @inline final def countBytesIn(sink: Sink[Long]): OutputStream = ByteCountOutputStream(self, sink)
  }
}
