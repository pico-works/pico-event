package org.pico.event

import java.util.concurrent.atomic.AtomicReference

trait ComputedView[A] extends View[A] {
  private val ref = new AtomicReference[A](compute())

  def compute(): A

  final def invalidate(): Unit = invalidations.invalidate()

  final override def value: A = {
    if (invalidations.valid) {
      ref.get()
    } else {
      invalidations.validate()
      val newValue = compute()
      ref.set(newValue)
      newValue
    }
  }

  final override val invalidations: Invalidations = Invalidations()
}
