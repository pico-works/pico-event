package org.pico.event.concurrent

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{Semaphore, TimeUnit}

import org.pico.disposal.Auto
import org.pico.disposal.std.autoCloseable._
import org.pico.event.Bus
import org.pico.event.syntax.sinkSource._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext.Implicits.global

class ExecutionContextBusSpec extends Specification {
  "Can use ExecutionContextBus" in {
    val semaphore = new Semaphore(0)
    val valueRef = new AtomicInteger(0)

    for {
      bus1  <- Auto(Bus[Int])
      bus2  <- Auto(bus1.via(ExecutionContextBus[Int]))
      _     <- Auto {
        bus2.subscribe { v =>
          valueRef.set(v)
          semaphore.release()
        }
      }
    } {
      bus1.publish(1)
      semaphore.tryAcquire(1, TimeUnit.SECONDS)
      valueRef.get ==== 1
    }
  }
}
