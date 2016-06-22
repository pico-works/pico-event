package org.pico.event

import org.specs2.mutable.Specification

class ClosedSinkSpec extends Specification {
  "ClosedSink" should {
    "allow publish of any type" in {
      ClosedSink.publish(())
      ClosedSink.publish(1)
      ClosedSink.publish(true)
      ok
    }

    "implement comap that returns self" in {
      ClosedSink.comap[Any](identity) must_=== ClosedSink
    }
  }
}
