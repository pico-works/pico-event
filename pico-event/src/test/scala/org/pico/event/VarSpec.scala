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
      val cell1 = Cell(1)
      val view1 = cell1.map(_ * 10)
      System.gc()

      view1.value must_=== 10
      cell1.value = 2
      view1.value must_=== 20
      cell1.value = 3
      view1.value must_=== 30
    }

    "have flatMap operation" in {
      val cell1 = Cell(0)
      val cell2 = Cell(0)

      val result = for {
        a <- cell1.asView
        b <- cell2.asView
      } yield a + b

      System.gc()

      result.value must_=== 0
      cell1.value = 2
      result.value must_=== 2
      cell2.value = 3
      result.value must_=== 5
      cell1.value = 5
      result.value must_=== 8
    }

    "have applyIn operation on two arguments" in {
      val cell1 = Cell[Int](0)
      val view1 = cell1.map(_ + 1)

      System.gc()

      view1.value must_=== 1
      cell1.value = 1
      view1.value must_=== 2
      cell1.value = 2
      view1.value must_=== 3
      cell1.value = 3
      view1.value must_=== 4
    }

    "have applyIn operation on two arguments" in {
      val cell1 = Cell[Int](0)
      val view1 = cell1.map(_ + 1)

      System.gc()

      cell1.value = 1
      view1.value must_=== 2
    }

    "have a source that can be folded" in {
      val cell1 = Cell(0)
      val view1 = cell1.source.foldRight(0)(_ + _)
      System.gc()
      cell1.value = 1
      view1.value must_=== 1
    }

    "have applyIn operation on two arguments" in {
      val cell1 = Cell[Int => Int](identity)
      val cell2 = Cell[Int](0)

      val result = cell1.asView ap cell2.asView
      System.gc()

      result.value must_=== 0
      cell2.value = 1
      result.value must_=== 1
      cell1.value = _ + 10
      result.value must_=== 11
      cell2.value = 2
      result.value must_=== 12
      cell1.value = _ + 20
      result.value must_=== 22
    }

    "have applyIn operation on two arguments" in {
      val cell1 = Cell[Int](0)
      val cell2 = Cell[Int](0)

      val result = cell1.asView.map[Int => Int](x => y => x + y) ap cell2.asView
      System.gc()

      result.value must_=== 0
      cell1.value = 2
      result.value must_=== 2
      cell2.value = 3
      result.value must_=== 5
      cell1.value = 5
      result.value must_=== 8
    }

    "have applyIn operation on three arguments" in {
      val cell1 = Cell(0)
      val cell2 = Cell(0)
      val cell3 = Cell(0)

      val result = (cell1.asView, cell2.asView, cell3.asView).applyIn(_ + _ + _)
      System.gc()

      result.value must_=== 0
      cell1.value = 2
      result.value must_=== 2
      cell2.value = 3
      result.value must_=== 5
      cell1.value = 5
      result.value must_=== 8
    }

    "have applyIn operation on three arguments" in {
      val cell1 = Cell(0)
      val cell2 = Cell(0)
      val cell3 = Cell(0)
      val cell4 = Cell(0)

      val result = (cell1.asView, cell2.asView, cell3.asView, cell4.asView).applyIn(_ + _ + _ + _)
      System.gc()

      result.value must_=== 0
      cell1.value = 2
      result.value must_=== 2
      cell2.value = 3
      result.value must_=== 5
      cell1.value = 5
      result.value must_=== 8
    }

    "be able to be reset by Disposer" in {
      val disposer = Disposer()
      val view1 = disposer.resets(0, Cell(1))

      view1.value must_=== 1
      view1.value = 2
      view1.value must_=== 2
      disposer.dispose()
      view1.value must_=== 0
    }

    "be able to be able to getAndSet" in {
      val cell1 = Cell(1)
      val view1 = cell1.source.foldRight(List.empty[Int])(_ :: _)

      cell1.getAndSet(2) must_=== 1
      cell1.value must_=== 2
      view1.value must_=== List(2)
    }

    "be able to be able to compareAndSet" in {
      val cell1 = Cell(1)
      val view1 = cell1.source.foldRight(List.empty[Int])(_ :: _)

      cell1.compareAndSet(1, 2) must_=== true
      cell1.value must_=== 2
      view1.value must_=== List(2)

      cell1.compareAndSet(1, 3) must_=== false
      cell1.value must_=== 2
      view1.value must_=== List(2)
    }

    "be able to be able to update" in {
      val cell1 = Cell(1)
      val view1 = cell1.source.foldRight(List.empty[Int])(_ :: _)

      cell1.update(_ + 9) must_=== 1
      cell1.value must_=== 10
      view1.value must_=== List(10)
    }
  }
}
