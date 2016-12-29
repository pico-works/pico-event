package org.pico.event

import java.util.concurrent.atomic.AtomicLong

import org.pico.atomic.syntax.std.atomicLong._

final class LongCell(initial: Long) extends Cell[Long] {
  val valueRef = new AtomicLong(initial)

  override def value: Long = {
    invalidations.validate()
    valueRef.get()
  }

  override def value_=(that: Long): Unit = {
    valueRef.set(that)
    invalidations.invalidate()
  }

  override def update(f: Long => Long): (Long, Long) = {
    val result = valueRef.update(f)
    invalidations.invalidate()
    result
  }
  
  def updateIf(cond: Long => Boolean, f: Long => Long): Option[(Long, Long)] = {
    val result = valueRef.updateIf(cond, f)
    result.foreach(_ => invalidations.invalidate())
    result
  }
  
  override def getAndSet(a: Long): Long = {
    val result = valueRef.getAndSet(a)
    invalidations.invalidate()
    result
  }
  
  override def compareAndSet(expect: Long, update: Long): Boolean = {
    val result = valueRef.compareAndSet(expect, update)
    if (result) {
      invalidations.invalidate()
    }
    result
  }
  
  override lazy val invalidations: Invalidations = Invalidations()
  
  def incrementAndGet(): Long = {
    val result = valueRef.incrementAndGet()
    invalidations.invalidate()
    result
  }
  
  def decrementAndGet(): Long = {
    val result = valueRef.decrementAndGet()
    invalidations.invalidate()
    result
  }
  
  def getAndIncrementAnd(): Long = {
    val result = valueRef.getAndIncrement()
    invalidations.invalidate()
    result
  }
  
  def getAndDecrementAnd(): Long = {
    val result = valueRef.getAndDecrement()
    invalidations.invalidate()
    result
  }
  
  def addAndGet(that: Long): Long = {
    val result = valueRef.addAndGet(that)
    invalidations.invalidate()
    result
  }
  
  def getAndAdd(that: Long): Long = {
    val result = valueRef.getAndAdd(that)
    invalidations.invalidate()
    result
  }
}

object LongCell {
  def apply(initial: Long): LongCell = new LongCell(initial)
}
