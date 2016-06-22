package org.pico.event

import java.util.concurrent.atomic.AtomicReference

import org.pico.atomic.syntax.std.atomicReference._
import org.pico.disposal.{Closed, Disposer}
import org.pico.disposal.std.autoCloseable._
import org.pico.disposal.syntax.disposable._
import org.pico.event.syntax.source._
import org.pico.fp._

/** A value that may change over time.  There is also an event source that emits the new value every
  * time the value changes.
  */
trait Live[A] extends Disposer {
  /** Get the current value.
    */
  def value: A

  /** Get the source that emits the new value every time the value changes.
    */
  def source: Source[A]

  /** Get this as a Live object.
    */
  def asLive: Live[A] = this

  /** Map the value.
    *
    * @param f The mapping function
    * @tparam B The return type of the mapping function
    * @return New live object containing the mapped value
    */
  def map[B](f: A => B): Live[B] = source.foldRight(f(value))((a, _) => f(a))
}

object Live {
  /** Create a live object that never changes.
    *
    * @param constant The value to initialise with
    * @tparam A The type of the value to initialise with
    * @return New live object with the initialised constant value
    */
  def apply[A](constant: A): Live[A] = new Live[A] {
    override def value: A = constant
    override def source: Source[A] = ClosedSinkSource
  }

  /** Fold the event source into a value given the value's initial state.
    *
    * @param f The folding function
    * @param initial The initial state
    * @tparam B Type of the new value
    * @return The value.
    */
  def foldRight[A, B](self: Source[A])(initial: B)(f: (A, => B) => B): Live[B] = {
    val state = new AtomicReference[B](initial)

    new Live[B] { temp =>
      override def value: B = state.get

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

  implicit val monad_Live_D8tgCFF = new Monad[Live] {
    override def point[A](a: => A): Live[A] = Live(a)

    override def ap[A, B](fa: => Live[A])(ff: => Live[A => B]): Live[B] = {
      (ff.source or fa.source).foldRight(ff.value -> fa.value) { case (either, (ffv, fav)) =>
        either match {
          case Left(f)  => f -> fav
          case Right(a) => ffv -> a
        }
      }.map { case (f, a) => f(a) }
    }

    override def map[A, B](fa: Live[A])(f: A => B): Live[B] = fa.map(f)

    override def bind[A, B](fa: Live[A])(f: A => Live[B]): Live[B] = {
      val busB = Bus[B]
      val live = busB.latest(f(fa.value).value)
      val subscriptionRef = live.swapDisposes(Closed, new AtomicReference(f(fa.value).source.into(busB)))
      val lock = new Object

      live += fa.source.subscribe { a =>
        lock.synchronized {
          subscriptionRef.swap(Closed).dispose()
          val liveB = f(a)
          busB.publish(liveB.value)
          subscriptionRef.swap(liveB.source.into(busB))
        }
      }

      live
    }
  }
}
