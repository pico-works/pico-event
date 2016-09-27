package org.pico.event.performance

import org.pico.event._
import org.specs2.mutable.Specification

class ViewSpec extends Specification {
  "View" should {
    "have performant eventCount method" in {
      val bus = Bus[Long]

      val threads = (0L until 10L).map { threadId =>
        val thread = new Thread {
          override def run(): Unit = {
            for (j <- 0L until 100000L) {
              bus.publish(1)
            }
          }
        }

        thread.start()

        thread
      }

      threads.foreach(_.join())

      threads.foreach { thread =>
        thread.getState must_=== Thread.State.TERMINATED
      }

      ok
    }
  }
}
