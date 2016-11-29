package org.pico.event.concurrent

import org.pico.event.{Bus, Sink, SinkSource}

import scala.concurrent.ExecutionContext

object ExecutionContextBus {
  def apply[A](implicit ec: ExecutionContext): Bus[A] = {
    val target = Bus[A]

    val sink = Sink[A] { a =>
      ec.execute(new Runnable {
        override def run(): Unit = target.publish(a)
      })
    }

    SinkSource.from(sink, target)
  }
}
