package org.pico.event

import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.{Closed, Disposer}
import org.pico.event.syntax.source._
import org.specs2.mutable.Specification

class ClosedSourceSpec extends Specification {
  "ClosedSource" should {
    "allow publish of any type" in {
      ClosedSource.subscribe(_ => ()) must_=== Closed
    }

    "implement map that returns ClosedSource" in {
      ClosedSource.map(identity) must_=== ClosedSource
    }

    "implement effect that returns ClosedSource" in {
      ClosedSource.effect(identity) must_=== ClosedSource
    }

    "implement mapConcat that returns ClosedSource" in {
      ClosedSource.mapConcat(_ => Iterable.empty) must_=== ClosedSource
    }

    "implement merge that returns source that emits same events as `that`" in {
      val bus = Bus[Int]
      val source = ClosedSource.merge(bus)
      val view = source.latest(0)

      bus.publish(1)
      view.value must_=== 1
    }

    "implement foldRight that returns source that emits same events as `that`" in {
      val view = ClosedSource.foldRight(1)((_, b) => b)
      view.value must_=== 1
    }

    "implement foldRight that returns a view that never changes" in {
      val bus = Bus[Int]
      val view = bus.latest(0)
      ClosedSource.into(bus)
      view.value must_=== 0
    }

    "implement filter that returns ClosedSource" in {
      ClosedSource.filter(_ => true) must_=== ClosedSource
    }

    "implement or that returns source that emits same events as `that`" in {
      val bus = Bus[Int]
      val source = ClosedSource or bus
      val leftBus = Bus[Any]
      val rightView = source.divertLeft(leftBus).latest(0)
      val count = leftBus.eventCount
      val disposer = Disposer()
      disposer += leftBus.subscribe(_ => println("Hello"))
      bus.publish(1)
      bus.publish(10)
      rightView.value must_=== 10
      count.value must_=== 0
    }
  }
}
