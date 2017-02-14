package org.pico.event.io


import java.io.Writer

import org.pico.event.{ClosedSink, Sink}

object NewlineCountWriter {
  /** Decorate a writer with a version that counts how many new lines have been written.
    */
  def apply(
             os: Writer,
             newLines: Sink[Long] = ClosedSink): Writer = {
    new Writer {
      override def flush(): Unit = os.flush()

      override def write(cbuf: Array[Char], off: Int, len: Int): Unit = {
        val end = off + len

        var i = off

        while (i < end) {
          if (cbuf(i) == '\n') {
            newLines.publish(1L)
          }

          i += 1
        }

        os.write(cbuf, off, len)
      }

      override def close(): Unit = os.close()
    }
  }
}
