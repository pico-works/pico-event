package org.pico.event

import java.util.concurrent.atomic.AtomicReference

import org.pico.atomic.syntax.std.atomicReference._

trait Var[A] extends Live[A] {
  def value_=(newValue: A): A

  def getAndSet(a: A): A

  def compareAndSet(expect: A, update: A): Boolean

  def update(f: A => A): A
}

object Var {
  def apply[A](initial: A): Var[A] = {
    val state = new AtomicReference[A](initial)

    new Var[A] { temp =>
      val _source = Bus[A]

      override val source = _source

      override def value: A = state.get

      override def value_=(replacement: A): A = {
        state.set(replacement)
        source.publish(replacement)
        replacement
      }

      override def getAndSet(replacement: A): A = {
        val oldValue = state.getAndSet(replacement)
        source.publish(value)
        oldValue
      }

      override def compareAndSet(expected: A, replacement: A): Boolean = {
        if (state.compareAndSet(expected, replacement)) {
          source.publish(replacement)
          true
        } else {
          false
        }
      }

      override def update(f: A => A): A = {
        val (oldValue, newValue) = state.update(f)
        source.publish(newValue)
        oldValue
      }
    }
  }
}
