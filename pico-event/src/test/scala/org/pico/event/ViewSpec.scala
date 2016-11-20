package org.pico.event

import org.pico.event.syntax.source._
import org.specs2.mutable.Specification
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.apply._
import cats.syntax.cartesian._

class ViewSpec extends Specification {
  "View" should {
    "have apply method that creates constant" in {
      val view = View(1)
      view.value must_=== 1
    }

    "have map operation" in {
      val bus = Bus[Int]
      val view1 = bus.latest(1)
      val view2 = view1.map(_ * 10)
      System.gc()

      view1.value must_=== 1
      view2.value must_=== 10

      bus.publish(2)
      view1.value must_=== 2
      view2.value must_=== 20

      bus.publish(3)
      view1.value must_=== 3
      view2.value must_=== 30
    }

//    "have flatMap operation" in {
//      val bus1 = Bus[Int]
//      val bus2 = Bus[Int]
//      val view1 = bus1.latest(0)
//      val view2 = bus2.latest(0)
//
//      val result = for {
//        a <- view1
//        b <- view2
//      } yield a + b
//
//      System.gc()
//
//      result.value must_=== 0
//      bus1.publish(2)
//      result.value must_=== 2
//      bus2.publish(3)
//      result.value must_=== 5
//      bus1.publish(5)
//      result.value must_=== 8
//    }

    "have applyIn operation on two arguments" in {
      val cell1 = Cell[Int](0)
      val view1 = cell1.asView.map(_ + 1)

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
      val view1 = cell1.asView.map(_ + 1)

      System.gc()

      cell1.value = 1
      view1.value must_=== 2
    }

    "have a source that can be folded" in {
      val cell1 = Cell(0)
      val view1 = cell1.asView
      cell1.value = 1
      view1.value must_=== 1
      cell1.value = 2
      view1.value must_=== 2
    }

    "have applyIn operation on two arguments" in {
      val cell1 = Cell[Int => Int](identity)
      val cell2 = Cell[Int](0)
      val view1 = cell1.asView
      val view2 = cell2.asView

      val result = view1 ap view2
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

    "have applyIn operation on three arguments" in {
      val bus1 = Bus[Int]
      val bus2 = Bus[Int]
      val bus3 = Bus[Int]
      val view1 = bus1.latest(0)
      val view2 = bus2.latest(0)
      val view3 = bus3.latest(0)

      val result = (view1 |@| view2 |@| view3).map(_ + _ + _)
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
      val view1 = bus1.latest(0)
      val view2 = bus2.latest(0)
      val view3 = bus3.latest(0)
      val view4 = bus4.latest(0)

      val result = (view1 |@| view2 |@| view3 |@| view4).map(_ + _ + _ + _)
      System.gc()

      result.value must_=== 0
      bus1.publish(2)
      result.value must_=== 2
      bus2.publish(3)
      result.value must_=== 5
      bus1.publish(5)
      result.value must_=== 8
    }

    "have asView method" in {
      val view = View(1)
      view.value must_== view.asView.value
    }

    "have point syntax" in {
      1.pure[View].value must_=== 1
    }

    "Should run quickly" in {
      val bus = Bus[Long]
      val count = bus.eventCount

      val before = System.currentTimeMillis()

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

      threads.foreach(_.join)

      val after = System.currentTimeMillis()

      val time = after - before

      ok
    }
  }
}
