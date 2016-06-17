package org.pico.event

import org.specs2.mutable.Specification

class VarSpec extends Specification {
  "Var" should {
    "have a source" >> {
      "that can be folded" in {
        val var1 = Var(0)
        val live1 = var1.source.foldRight(0)(_ + _)
        System.gc()
        var1.value = 1
        live1.value must_=== 1
      }
    }
  }
}
