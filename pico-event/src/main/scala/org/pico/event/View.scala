package org.pico.event

import cats.Applicative
import org.pico.disposal.SimpleDisposer
import org.pico.disposal.std.autoCloseable._

@specialized(Boolean, Long, Double)
trait View[+A] extends SimpleDisposer { self =>
  def asView: View[A] = this

  def value: A

  def invalidations: Source[Unit]
}

object View {
  implicit val applicativeView_JZ4YLf6 = new Applicative[View] {
    override def map[A, B](fa: View[A])(f: A => B): View[B] = {
      new ComputedView[B] {
        this.disposes(fa.invalidations.subscribe(_ => invalidate()))

        override def compute(): B = f(fa.value)
      }
    }

    override def pure[A](x: A): View[A] = View(x)

    override def ap[A, B](ff: View[A => B])(fa: View[A]): View[B] = {
      new ComputedView[B] {
        this.disposes(fa.invalidations.subscribe(_ => invalidate()))
        this.disposes(ff.invalidations.subscribe(_ => invalidate()))

        override def compute(): B = ff.value(fa.value)
      }
    }

//    override def flatMap[A, B](fa: View[A])(f: A => View[B]): View[B] = {
//      new ComputedView[B] {
//        override def compute(): B = f(fa.value).value
//
//        val subscriptionRef = this.swapDisposes(Closed, new AtomicReference(f(fa.value).invalidations.subscribe(_ -> invalidations.invalidate())))
//        val lock = new Object
//
//        this += fa.invalidations.subscribe { _ =>
//          lock.synchronized {
//            subscriptionRef.swap(Closed).dispose()
//            val viewB = f(fa.value)
//            subscriptionRef.swap(viewB.invalidations.subscribe(_ -> invalidations.invalidate()))
//          }
//        }
//      }
//    }

//    override def tailRecM[A, B](a: A)(f: A => View[Either[A, B]]): View[B] = defaultTailRecM(a)(f)
  }

  def apply[A](initial: A): View[A] = {
    new View[A] {
      override def value: A = initial

      override def invalidations: Source[Unit] = ClosedSource
    }
  }
}
