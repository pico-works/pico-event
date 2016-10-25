package org.pico.event

import java.util.concurrent.atomic.AtomicBoolean

trait Invalidations extends Source[Unit] {
  def valid: Boolean

  def invalid: Boolean = !valid

  def validate(): Unit

  def invalidate(): Unit
}

object Invalidations {
  def apply(): Invalidations = {
    new Invalidations with SimpleSinkSource[Unit, Unit] {
      val isValid = new AtomicBoolean(false)

      def valid = isValid.get()

      def validate(): Unit = isValid.set(true)

      override def invalidate(): Unit = {
        if (isValid.getAndSet(false)) {
          this.publish(())
        }
      }

      override def transform: Unit => Unit = identity
    }
  }
}
