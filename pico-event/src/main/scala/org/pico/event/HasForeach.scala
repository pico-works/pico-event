package org.pico.event

import scala.language.higherKinds

trait HasForeach[F[_]] {
  def foreach[A](self: F[A])(f: A => Unit): Unit
}
