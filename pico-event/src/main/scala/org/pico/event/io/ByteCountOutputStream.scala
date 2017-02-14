package org.pico.event.io

import java.io.OutputStream

import org.pico.event.Sink

object ByteCountOutputStream {
  /** Decorate an output stream with a version that counts how many bytes have been written.
    */
  def apply(
     os: OutputStream,
     bytesWritten: Sink[Long]): OutputStream = {
    new OutputStream {
      override def write(byte: Int): Unit = {
        os.write(byte)
        bytesWritten.publish(1L)
      }

      override def write(buffer: Array[Byte]): Unit = {
        os.write(buffer)
        bytesWritten.publish(buffer.length.toLong)
      }

      override def write(buffer: Array[Byte], offset: Int, length: Int): Unit = {
        os.write(buffer, offset, length)
        bytesWritten.publish(length.toLong)
      }

      override def flush(): Unit = os.flush()

      override def close(): Unit = os.close()
    }
  }
}
