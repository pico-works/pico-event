package org.pico.event

import java.io.Closeable

import cats.Applicative
import org.pico.disposal.std.autoCloseable._

trait Val[A] extends Closeable { self =>
  def value: A

  def invalidations: Source[Unit]
}

object Val {
  implicit val applicativeVal_JZ4YLf6 = new Applicative[Val] {
    override def map[A, B](fa: Val[A])(f: A => B): Val[B] = {
      new ComputedVal[B] {
        this.disposes(fa.invalidations.subscribe(_ => invalidate()))

        override def compute(): B = f(fa.value)
      }
    }

    override def pure[A](x: A): Val[A] = Val(x)

    override def ap[A, B](ff: Val[A => B])(fa: Val[A]): Val[B] = {
      new ComputedVal[B] {
        this.disposes(fa.invalidations.subscribe(_ => invalidate()))
        this.disposes(ff.invalidations.subscribe(_ => invalidate()))

        override def compute(): B = ff.value(fa.value)
      }
    }
  }

  def apply[A](initial: A): Val[A] = {
    new Val[A] {
      override def value: A = initial

      override def invalidations: Source[Unit] = ClosedSource

      override def close(): Unit = ()
    }
  }
}
