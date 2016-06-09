package org.pico.event

object Bus {
  /** Create a Bus of the given type.
    */
  def apply[A]: Bus[A] = SinkSource[A, A](identity)
}
