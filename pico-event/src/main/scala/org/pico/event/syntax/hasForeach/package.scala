package org.pico.event.syntax

import org.pico.event.HasForeach

import scala.language.higherKinds

package object hasForeach {
  implicit class HasForeachOps_fX9WgUY6[F[_], A](val self: F[A]) extends AnyVal {
    @inline def foreach(f: A => Unit)(implicit ev: HasForeach[F]): Unit = ev.foreach(self)(f)
  }
}
