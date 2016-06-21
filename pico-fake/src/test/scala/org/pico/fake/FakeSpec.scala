package org.pico.fake

import org.specs2.mutable.Specification

class FakeSpec extends Specification {
  "Fake" in {
    Fake.touch()
    success
  }
}
