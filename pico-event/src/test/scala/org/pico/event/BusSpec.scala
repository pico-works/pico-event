package org.pico.event

import java.util.concurrent.atomic.AtomicInteger

import org.pico.disposal.Disposer
import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._
import org.pico.event.syntax.source._
import org.specs2.mutable.Specification

class BusSpec extends Specification {
  "Bus" should {
    "have map method that invokes function one for every publish when there is one subscriber" in {
      val bus = Bus[Int]
      val counter = new AtomicInteger(0)
      val source = bus.map{e => counter.addAndGet(e); e}
      val disposer = Disposer()
      disposer += source.subscribe(e => ())
      System.gc()

      bus.publish(1)
      bus.publish(1)
      bus.publish(1)

      counter.get() must_=== 3
    }

    "have map method that invokes function one for every publish when there are two subscribers" in {
      val bus = Bus[Int]
      val counter = new AtomicInteger(0)
      val source = bus.map{e => counter.addAndGet(e); e}
      val disposer = Disposer()
      disposer += source.subscribe(e => ())
      disposer += source.subscribe(e => ())
      System.gc()

      bus.publish(1)
      bus.publish(1)
      bus.publish(1)

      counter.get() must_=== 3
    }

    "should implement map" ! {
      val disposer = Disposer()
      val es = Bus[Int]
      val fs = es.map(_ + 1)
      var esValues = List.empty[Int]
      var fsValues = List.empty[Int]
      disposer += es.subscribe(v => esValues ::= v)
      disposer += fs.subscribe(v => fsValues ::= v)
      System.gc()
      es.publish(10)
      esValues must_=== List(10)
      fsValues must_=== List(11)
      System.gc()
      es.publish(20)
      esValues must_=== List(20, 10)
      fsValues must_=== List(21, 11)
    }

    "should be able to mapConcat using an iterable producing function to create a source that emits elements" ! {
      val aBus = Bus[(Int, String)]
      val bBus = Bus[String]

      val disposer = Disposer()

      disposer += aBus.mapConcat { case (n, s) => List.fill(n)(s) } into bBus

      val result = bBus.foldRight(List.empty[String])(_ :: _)
      System.gc()

      aBus.publish(2 -> "A")
      aBus.publish(1 -> "B")
      aBus.publish(3 -> "C")

      result.value must_=== List("C", "C", "C", "B", "A", "A")
    }

    "should be able to divert left of either into sink" ! {
      val inBus = Bus[Either[String, Int]]
      val ltBus = Bus[String]
      val rtBus = Bus[Int]

      val disposer = Disposer()

      disposer += inBus.divertLeft(ltBus).into(rtBus)

      val ltValue = ltBus.foldRight(List.empty[String])(_ :: _)
      val rtValue = rtBus.foldRight(List.empty[Int])(_ :: _)
      System.gc()

      inBus.publish(Left("A"))
      inBus.publish(Right(1))
      inBus.publish(Left("B"))
      inBus.publish(Right(2))

      ltValue.value must_=== List("B", "A")
      rtValue.value must_=== List(2, 1)
    }

    "should be able to divert right of either into sink" ! {
      val inBus = Bus[Either[String, Int]]
      val ltBus = Bus[String]
      val rtBus = Bus[Int]

      val disposer = Disposer()

      disposer += inBus.divertRight(rtBus).into(ltBus)

      val ltValue = ltBus.foldRight(List.empty[String])(_ :: _)
      val rtValue = rtBus.foldRight(List.empty[Int])(_ :: _)
      System.gc()

      inBus.publish(Left("A"))
      inBus.publish(Right(1))
      inBus.publish(Left("B"))
      inBus.publish(Right(2))

      ltValue.value must_=== List("B", "A")
      rtValue.value must_=== List(2, 1)
    }

    "should implement into method that creates a subscription on source that writes to sink" ! {
      val aBus = Bus[Int]
      val bBus = Bus[Int]
      val cBus = Bus[Int]

      val disposer = Disposer()
      disposer += aBus into bBus
      disposer += bBus into cBus
      val result = cBus.foldRight(List.empty[Int])(_ :: _)
      System.gc()

      aBus.publish(1)
      aBus.publish(2)

      result.value must_=== List(2, 1)

      disposer.close()

      aBus.publish(3)
      bBus.publish(4)

      result.value must_=== List(2, 1)
    }

    "should implement merge method such that merged source emits the same events as both underlying sources" ! {
      val ltBus = Bus[Int]
      val rtBus = Bus[Int]
      val combinedBus = ltBus merge rtBus
      val result = combinedBus.foldRight(List.empty[Int])(_ :: _)
      System.gc()
      ltBus.publish(0)
      rtBus.publish(1)
      ltBus.publish(2)
      rtBus.publish(3)
      result.value must_=== List(3, 2, 1, 0)
    }

    "Disposed source should no longer emit events" ! {
      val bus = Bus[Int]
      val source = bus.map(_ * 100)
      val result1 = source.foldRight(List.empty[Int])(_ :: _)
      bus.publish(0)
      source.dispose()
      bus.publish(1)
      val result2 = source.foldRight(List.empty[Int])(_ :: _)
      System.gc()
      bus.publish(2)
      result1.value must_=== List(0)
      result2.value must_=== List()
    }

    "Disposed sink should no longer publish events" ! {
      val bus = Bus[Int]
      val sink: Sink[Int] = bus.comap(_ * 100)
      val result1 = bus.foldRight(List.empty[Int])(_ :: _)
      System.gc()
      sink.publish(0)
      sink.dispose()
      sink.publish(1)
      result1.value must_=== List(0)
    }

    "Disposed bus should no longer emit or publish events" ! {
      val bus = Bus[Int]
      val result = bus.foldRight(List.empty[Int])(_ :: _)
      System.gc()
      bus.publish(0)
      bus.dispose()
      bus.publish(1)
      result.value must_=== List(0)
    }
  }
}
