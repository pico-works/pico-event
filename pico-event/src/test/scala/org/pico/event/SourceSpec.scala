package org.pico.event

import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._
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
  }
}
