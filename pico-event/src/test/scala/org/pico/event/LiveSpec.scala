package org.pico.event

import org.specs2.mutable.Specification
import org.pico.event.syntax.source._
import org.pico.fp.syntax._

class LiveSpec extends Specification {
  "Live" should {
    "have map operation" in {
      val bus = Bus[Int]
      val live1 = bus.latest(1)
      val live2 = live1.map(_ * 10)

      live1.value must_=== 1
      live2.value must_=== 10

      bus.publish(2)
      live1.value must_=== 2
      live2.value must_=== 20

      bus.publish(3)
      live1.value must_=== 3
      live2.value must_=== 30
    }

    "have flatMap operation" in {
      val bus1 = Bus[Int]
      val bus2 = Bus[Int]
      val live1 = bus1.latest(0)
      val live2 = bus2.latest(0)

      val result = for {
        a <- live1
        b <- live2
      } yield a + b

      result.value must_=== 0
      bus1.publish(2)
      result.value must_=== 2
      bus2.publish(3)
      result.value must_=== 5
      bus1.publish(5)
      result.value must_=== 8
    }
  }
}
