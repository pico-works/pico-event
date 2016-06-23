package org.pico.event

import org.pico.disposal.Closed
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
      val live = source.latest(0)

      bus.publish(1)
      live.value must_=== 1
    }

    "implement foldRight that returns source that emits same events as `that`" in {
      val live = ClosedSource.foldRight(1)((_, b) => b)
      live.value must_=== 1
    }

    "implement foldRight that returns Live object that never changes" in {
      val bus = Bus[Int]
      val live = bus.latest(0)
      ClosedSource.into(bus)
      live.value must_=== 0
    }

    "implement filter that returns ClosedSource" in {
      ClosedSource.filter(_ => true) must_=== ClosedSource
    }

    "implement or that returns source that emits same events as `that`" in {
      val bus = Bus[Int]
      val source = ClosedSource or bus
      val leftBus = Bus[Any]
      val rightLive = source.divertLeft(leftBus).latest(0)
      val count = leftBus.eventCount
      val xxx = leftBus.subscribe(_ => println("Hello"))
      bus.publish(1)
      bus.publish(10)
      rightLive.value must_=== 10
      count.value must_=== 0
    }
  }
}
