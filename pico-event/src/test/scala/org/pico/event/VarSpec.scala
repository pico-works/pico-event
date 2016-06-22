package org.pico.event

import org.pico.disposal.Disposer
import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._
import org.pico.event.syntax.disposer._
import org.pico.fp.syntax._
import org.specs2.mutable.Specification

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
        a <- live1.asLive
        b <- live2.asLive
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

      val result = var1.asLive ap var2.asLive
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

      val result = var1.asLive.map[Int => Int](x => y => x + y) ap var2.asLive
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

      val result = (var1.asLive, var2.asLive, var3.asLive).applyIn(_ + _ + _)
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

      val result = (var1.asLive, var2.asLive, var3.asLive, var4.asLive).applyIn(_ + _ + _ + _)
      System.gc()

      result.value must_=== 0
      var1.value = 2
      result.value must_=== 2
      var2.value = 3
      result.value must_=== 5
      var1.value = 5
      result.value must_=== 8
    }

    "be able to be reset by Disposer" in {
      val disposer = Disposer()
      val v1 = disposer.resets(0, Var(1))

      v1.value must_=== 1
      v1.value = 2
      v1.value must_=== 2
      disposer.dispose()
      v1.value must_=== 0
    }

    "be able to be able to getAndSet" in {
      val v1 = Var(1)
      val v2 = v1.source.foldRight(List.empty[Int])(_ :: _)

      v1.getAndSet(2) must_=== 1
      v1.value must_=== 2
      v2.value must_=== List(2)
    }

    "be able to be able to compareAndSet" in {
      val v1 = Var(1)
      val v2 = v1.source.foldRight(List.empty[Int])(_ :: _)

      v1.compareAndSet(1, 2) must_=== true
      v1.value must_=== 2
      v2.value must_=== List(2)

      v1.compareAndSet(1, 3) must_=== false
      v1.value must_=== 2
      v2.value must_=== List(2)
    }

    "be able to be able to update" in {
      val v1 = Var(1)
      val v2 = v1.source.foldRight(List.empty[Int])(_ :: _)

      v1.update(_ + 9) must_=== 1
      v1.value must_=== 10
      v2.value must_=== List(10)
    }
  }
}
