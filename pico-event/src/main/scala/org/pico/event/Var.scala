package org.pico.event

import java.util.concurrent.atomic.AtomicReference

trait Var[A] extends Val[A] {
  def value: A

  def value_=(that: A): Unit
}

object Var {
  def apply[A](initial: A): Var[A] = {
    new Var[A] {
      val valueRef = new AtomicReference[A](initial)

      override def value: A = valueRef.get()

      override def value_=(that: A): Unit = {
        valueRef.set(that)
        invalidations.publish(())
      }

      override lazy val invalidations: Bus[Unit] = Bus[Unit]

      override def close(): Unit = ()
    }
  }
}
