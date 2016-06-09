package org.pico.event

trait SimpleBus[A] extends SimpleSinkSource[A, A] {
  override def transform: A => A = identity
}
