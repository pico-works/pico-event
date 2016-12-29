package org.pico.event

import java.util.concurrent.atomic.AtomicReference

import org.pico.atomic.syntax.std.atomicReference._

@specialized(Boolean, Long, Double)
trait Cell[A] extends View[A] {
  def value: A

  def value_=(that: A): Unit

  def update(f: A => A): (A, A)
  
  def updateIf(cond: A => Boolean, f: A => A): Option[(A, A)]

  def getAndSet(a: A): A

  def compareAndSet(expect: A, update: A): Boolean
}

object Cell {
  def apply[A](initial: A): Cell[A] = {
    new Cell[A] {
      val valueRef = new AtomicReference[A](initial)

      override def value: A = {
        invalidations.validate()
        valueRef.get()
      }

      override def value_=(that: A): Unit = {
        valueRef.set(that)
        invalidations.invalidate()
      }

      override def update(f: A => A): (A, A) = {
        val result = valueRef.update(f)
        invalidations.invalidate()
        result
      }
  
      override def updateIf(cond: A => Boolean, f: A => A): Option[(A, A)] = {
        val result = valueRef.updateIf(cond, f)
        result.foreach(_ => invalidations.invalidate())
        result
      }

      override def getAndSet(a: A): A = {
        val result = valueRef.getAndSet(a)
        invalidations.invalidate()
        result
      }

      override def compareAndSet(expect: A, update: A): Boolean = {
        val result = valueRef.compareAndSet(expect, update)
        if (result) {
          invalidations.invalidate()
        }
        result
      }

      override lazy val invalidations: Invalidations = Invalidations()
    }
  }
}
