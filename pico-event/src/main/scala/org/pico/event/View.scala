package org.pico.event

import java.util.concurrent.atomic.AtomicReference

import org.pico.atomic.syntax.std.atomicReference._
import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._
import org.pico.disposal.{Closed, SimpleDisposer}
import org.pico.event.syntax.source._
import org.pico.fp._

/** A value that may change over time.  There is also an event source that emits the new value every
  * time the value changes.
  */
trait View[A] extends SimpleDisposer {
  /** Get the current value.
    */
  def value: A

  /** Get the source that emits the new value every time the value changes.
    */
  def source: Source[A]

  /** Get this as a View object.
    */
  def asView: View[A] = this

  /** Map the value.
    *
    * @param f The mapping function
    * @tparam B The return type of the mapping function
    * @return New view containing the mapped value
    */
  def map[B](f: A => B): View[B] = source.foldRight(f(value))((a, _) => f(a))
}

object View {
  /** Create a view that never changes.
    *
    * @param constant The value to initialise with
    * @tparam A The type of the value to initialise with
    * @return New view with the initialised constant value
    */
  @inline
  final def apply[A](constant: A): View[A] = new View[A] {
    override def value: A = constant
    override def source: Source[A] = ClosedSource
  }

  /** Fold the event source into a value given the value's initial state.
    *
    * @param f The folding function
    * @param initial The initial state
    * @tparam B Type of the new value
    * @return The value.
    */
  @inline
  final def foldRight[A, B](self: Source[A])(initial: B)(f: (A, => B) => B): View[B] = {
    val state = new AtomicReference[B](initial)

    new View[B] { temp =>
      override def value: B = state.value

      override val source = Bus[B]

      this.releases(self)
      source.releases(this)

      temp.disposes {
        self.subscribe { e =>
          val (_, newValue) = state.update(v => f(e, v))
          source.publish(newValue)
        }
      }
    }
  }

  implicit val monad_View_D8tgCFF = new Monad[View] {
    override def point[A](a: => A): View[A] = View(a)

    override def ap[A, B](fa: => View[A])(ff: => View[A => B]): View[B] = {
      (ff.source or fa.source).foldRight(ff.value -> fa.value) { case (either, (ffv, fav)) =>
        either match {
          case Left(f)  => f -> fav
          case Right(a) => ffv -> a
        }
      }.map { case (f, a) => f(a) }
    }

    override def map[A, B](fa: View[A])(f: A => B): View[B] = fa.map(f)

    override def bind[A, B](fa: View[A])(f: A => View[B]): View[B] = {
      val busB = Bus[B]
      val view = busB.latest(f(fa.value).value)
      val subscriptionRef = view.swapDisposes(Closed, new AtomicReference(f(fa.value).source.into(busB)))
      val lock = new Object

      view += fa.source.subscribe { a =>
        lock.synchronized {
          subscriptionRef.swap(Closed).dispose()
          val viewB = f(a)
          busB.publish(viewB.value)
          subscriptionRef.swap(viewB.source.into(busB))
        }
      }

      view
    }
  }
}
