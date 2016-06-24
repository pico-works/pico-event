package org.pico.event

import org.pico.event.syntax.source._
import org.pico.fp.syntax._
import org.specs2.mutable.Specification

class LiveSpec extends Specification {
  "Live" should {
    "have apply method that creates constant" in {
      val live = Live(1)
      live.value must_=== 1
      live.source must_=== ClosedSource
    }

    "have map operation" in {
      val bus = Bus[Int]
      val live1 = bus.latest(1)
      val live2 = live1.map(_ * 10)
      System.gc()

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

      System.gc()

      result.value must_=== 0
      bus1.publish(2)
      result.value must_=== 2
      bus2.publish(3)
      result.value must_=== 5
      bus1.publish(5)
      result.value must_=== 8
    }

    "have applyIn operation on two arguments" in {
      val var1 = Var[Int](0)
      val live1 = var1.map(_ + 1)

      System.gc()

      live1.value must_=== 1
      var1.value = 1
      live1.value must_=== 2
      var1.value = 2
      live1.value must_=== 3
      var1.value = 3
      live1.value must_=== 4
    }

    "have applyIn operation on two arguments" in {
      val var1 = Var[Int](0)
      val live1 = var1.map(_ + 1)

      System.gc()

      var1.value = 1
      live1.value must_=== 2
    }

    "have a source that can be folded" in {
      val var1 = Var(0)
      val live1 = var1.source.foldRight(0)((a, _) => a)
      var1.value = 1
      live1.value must_=== 1
      var1.value = 2
      live1.value must_=== 2
      ok
    }

    "have applyIn operation on two arguments" in {
      val var1 = Var[Int => Int](identity)
      val var2 = Var[Int](0)
      val live1: Live[Int => Int] = var1
      val live2: Live[Int] = var2

      val result = live1 ap live2
      System.gc()

      result.value must_=== 0
      var2.value = 1
      result.value must_=== 1
      var1.value = _ + 10
      result.value must_=== 11
      var2.value = 2
      result.value must_=== 12
      var1.value = _ + 20
      result.value must_=== 22
    }

    "have applyIn operation on two arguments" in {
      val var1 = Var[Int](0)
      val var2 = Var[Int](0)
      val live1: Live[Int] = var1
      val live2: Live[Int] = var2

      val result = live1.map[Int => Int](x => y => x + y) ap live2
      System.gc()

      result.value must_=== 0
      var1.value = 2
      result.value must_=== 2
      var2.value = 3
      result.value must_=== 5
      var1.value = 5
      result.value must_=== 8
    }

    "have applyIn operation on three arguments" in {
      val bus1 = Bus[Int]
      val bus2 = Bus[Int]
      val bus3 = Bus[Int]
      val live1 = bus1.latest(0)
      val live2 = bus2.latest(0)
      val live3 = bus3.latest(0)

      val result = (live1, live2, live3).applyIn(_ + _ + _)
      System.gc()

      result.value must_=== 0
      bus1.publish(2)
      result.value must_=== 2
      bus2.publish(3)
      result.value must_=== 5
      bus1.publish(5)
      result.value must_=== 8
    }

    "have applyIn operation on three arguments" in {
      val bus1 = Bus[Int]
      val bus2 = Bus[Int]
      val bus3 = Bus[Int]
      val bus4 = Bus[Int]
      val live1 = bus1.latest(0)
      val live2 = bus2.latest(0)
      val live3 = bus3.latest(0)
      val live4 = bus4.latest(0)

      val result = (live1, live2, live3, live4).applyIn(_ + _ + _ + _)
      System.gc()

      result.value must_=== 0
      bus1.publish(2)
      result.value must_=== 2
      bus2.publish(3)
      result.value must_=== 5
      bus1.publish(5)
      result.value must_=== 8
    }

    "have asLive method" in {
      val v = Live(1)
      v.value must_== v.asLive.value
    }

    "have closed source if initialised as constant" in {
      Live(1).source must_=== ClosedSource
    }

    "have point syntax" in {
      1.point[Live].value must_=== 1
    }
  }
}
