package org.pico.event

import org.specs2.mutable.Specification
import org.pico.fp.syntax._

class VarSpec extends Specification {
  "Var" should {
    "have map operation" in {
      val var1 = Var(1)
      val live2 = var1.map(_ * 10)
      System.gc()

      live2.value must_=== 10
      var1.value = 2
      live2.value must_=== 20
      var1.value = 3
      live2.value must_=== 30
    }

    "have flatMap operation" in {
      val live1 = Var(0)
      val live2 = Var(0)

      val result = for {
        a <- live1.live
        b <- live2.live
      } yield a + b

      System.gc()

      result.value must_=== 0
      live1.value = 2
      result.value must_=== 2
      live2.value = 3
      result.value must_=== 5
      live1.value = 5
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
      val live1 = var1.source.foldRight(0)(_ + _)
      System.gc()
      var1.value = 1
      live1.value must_=== 1
    }

    "have applyIn operation on two arguments" in {
      val var1 = Var[Int => Int](identity)
      val var2 = Var[Int](0)

      val result = var1.live ap var2.live
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

      val result = var1.live.map[Int => Int](x => y => x + y) ap var2.live
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
      val var1 = Var(0)
      val var2 = Var(0)
      val var3 = Var(0)

      val result = (var1.live, var2.live, var3.live).applyIn(_ + _ + _)
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
      val var1 = Var(0)
      val var2 = Var(0)
      val var3 = Var(0)
      val var4 = Var(0)

      val result = (var1.live, var2.live, var3.live, var4.live).applyIn(_ + _ + _ + _)
      System.gc()

      result.value must_=== 0
      var1.value = 2
      result.value must_=== 2
      var2.value = 3
      result.value must_=== 5
      var1.value = 5
      result.value must_=== 8
    }
  }
}
