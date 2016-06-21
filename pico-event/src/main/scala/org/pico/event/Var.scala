package org.pico.event

import java.util.concurrent.atomic.AtomicReference

import org.pico.atomic.syntax.std.atomicReference._

/** An atomic mutable variable that can act as an event source.
  *
  * It always has a current value.
  *
  * @tparam A The type of the value the mutable variable stores
  */
trait Var[A] extends Live[A] {
  /** Assign a new value to the variable
    *
    * The new value will be emitted.
    *
    * @param newValue New new value
    * @return The value that was assigned
    */
  def value_=(newValue: A): A

  /** Atomically get and set a value to the variable.
    *
    * The new value will be emitted.
    *
    * @param a The new value
    * @return The value that was replaced
    */
  def getAndSet(a: A): A

  /** Atomically set the replacement value to the variable provided that the current value is
    * equal to the expected value.
    *
    * The new value will be emitted if the set was successful.
    *
    * @param expect The expected current value
    * @param replacement The replacement value
    * @return true if the replacement value was set
    */
  def compareAndSet(expect: A, replacement: A): Boolean

  /** Atomically update the existing value with the result of its application to the provided
    * function.  This method will retry until the value is set successfully.
    *
    * The new value will be emitted.
    *
    * @param f The transforming function.  Because of retries, this function should be inexpensive
    *          and must not have any side-effects.
    * @return The replaced value.
    */
  def update(f: A => A): A
}

object Var {
  /** Create an atomic mutable variable with an initial value that can act as an event source that
    * emits each newly assigned value.
    *
    * @param initial The initial value
    * @tparam A The type of the initial value
    * @return The atomic mutable variable
    */
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
