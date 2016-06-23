package org.pico.event

import org.pico.event.syntax.sink._
import org.specs2.mutable.Specification
import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._

class SinkSpec extends Specification {
  "Sink" should {
    "have filter operation" in {
      val bus = Bus[Int]
      val result = bus.foldRight(List.empty[Int])(_ :: _)
      val sink = bus.cofilter(_ % 2 == 0)
      result.value must_=== List.empty
      sink.publish(1)
      sink.publish(2)
      sink.publish(3)
      result.value must_=== List(2)
      sink.dispose()
      sink.publish(4)
      result.value must_=== List(2)
    }

    "have comap operation" in {
      val bus = Bus[Int]
      val result = bus.foldRight(List.empty[Int])(_ :: _)
      val sink = bus.comap[String](_.length)
      result.value must_=== List.empty
      sink.publish("1")
      sink.publish("2")
      sink.publish("3")
      result.value must_=== List(1, 1, 1)
      sink.dispose()
      sink.publish("4")
      result.value must_=== List(1, 1, 1)
    }
  }
}
