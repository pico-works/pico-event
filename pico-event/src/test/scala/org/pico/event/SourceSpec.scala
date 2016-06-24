package org.pico.event

import org.pico.disposal.Disposer
import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._
import org.pico.event.syntax.source._
import org.specs2.mutable.Specification

class SourceSpec extends Specification {
  "Source" should {
    "have filter operation" in {
      val bus = Bus[Int]
      val source = bus.filter(_ % 2 == 0)
      val result = source.foldRight(List.empty[Int])(_ :: _)
      System.gc()
      result.value must_=== List.empty
      bus.publish(1)
      bus.publish(2)
      bus.publish(3)
      result.value must_=== List(2)
      bus.dispose()
      bus.publish(4)
      result.value must_=== List(2)
    }

    "have map operation" in {
      val bus = Bus[Int]
      val source = bus.map(_ * 10)
      val result = source.foldRight(List.empty[Int])(_ :: _)
      System.gc()

      bus.publish(1)
      result.value must_=== List(10)
      bus.publish(2)
      result.value must_=== List(20, 10)
    }

    "have or operation" in {
      val bus1 = Bus[Int]
      val bus2 = Bus[String]
      val source1: Source[Int] = bus1
      val source2: Source[String] = bus2
      val source3: Source[Either[Int, String]] = source1 or source2
      val result = source3.foldRight(List.empty[Either[Int, String]])(_ :: _)
      System.gc()

      bus1.publish(1)
      bus2.publish("Hello")

      result.value must_=== List(Right("Hello"), Left(1))
    }

    "have count operation" in {
      val bus = Bus[Int]
      val count = bus.eventCount
      System.gc()

      count.value must_=== 0
      bus.publish(1)
      count.value must_=== 1
      bus.publish(1)
      count.value must_=== 2
    }

    "have effect operation" in {
      val bus = Bus[Int]
      val disposer = Disposer()
      var count = 0
      disposer += bus.effect(e => count += e)
      System.gc()

      count must_=== 0
      bus.publish(1)
      count must_=== 1
      bus.publish(2)
      count must_=== 3
    }

    "allow consecutive map operations" in {
      val bus = Bus[Int]
      val source = bus.map(_ * 100).map(_ + 10)
      val v = source.latest(0)
      System.gc()

      v.value must_=== 0
      bus.publish(1)
      v.value must_=== 110
      bus.publish(2)
      v.value must_=== 210
    }
  }
}
