package org.pico.event

import java.util.concurrent.atomic.AtomicInteger

import org.pico.atomic.syntax.std.atomicInteger._

final class IntCell(initial: Int) extends Cell[Int] {
  val valueRef = new AtomicInteger(initial)

  override def value: Int = {
    invalidations.validate()
    valueRef.get()
  }

  override def value_=(that: Int): Unit = {
    valueRef.set(that)
    invalidations.invalidate()
  }

  override def update(f: Int => Int): (Int, Int) = {
    val result = valueRef.update(f)
    invalidations.invalidate()
    result
  }

  override def getAndSet(a: Int): Int = {
    val result = valueRef.getAndSet(a)
    invalidations.invalidate()
    result
  }

  override def compareAndSet(expect: Int, update: Int): Boolean = {
    val result = valueRef.compareAndSet(expect, update)
    if (result) {
      invalidations.invalidate()
    }
    result
  }

  override lazy val invalidations: Invalidations = Invalidations()

  def incrementAndGet(): Int = {
    val result = valueRef.incrementAndGet()
    invalidations.invalidate()
    result
  }

  def decrementAndGet(): Int = {
    val result = valueRef.decrementAndGet()
    invalidations.invalidate()
    result
  }

  def getAndIncrementAnd(): Int = {
    val result = valueRef.getAndIncrement()
    invalidations.invalidate()
    result
  }

  def getAndDecrementAnd(): Int = {
    val result = valueRef.getAndDecrement()
    invalidations.invalidate()
    result
  }

  def addAndGet(that: Int): Int = {
    val result = valueRef.addAndGet(that)
    invalidations.invalidate()
    result
  }

  def getAndAdd(that: Int): Int = {
    val result = valueRef.getAndAdd(that)
    invalidations.invalidate()
    result
  }
}

object IntCell {
  def apply(initial: Int): IntCell = new IntCell(initial)
}
