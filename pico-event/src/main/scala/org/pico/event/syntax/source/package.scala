package org.pico.event.syntax

import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

import cats.kernel.Semigroup
import cats.{Foldable, Monoid}
import org.pico.disposal.std.autoCloseable._
import org.pico.event._
import org.pico.event.std.all._
import org.pico.event.syntax.future._

import scala.concurrent.{ExecutionContext, Future}

package object source {
  implicit class SourceOps_hJob2ex[A](val self: Source[A]) extends AnyVal {
    /** Create a view with an initial value, which will have the latest value that was
      * emitted by the event source.
      *
      * @param initial The initial value
      * @return The view that will change to contain the latest value emitted by the source
      */
    @inline
    final def latest(initial: A): View[A] = self.foldRight(initial)((v, _) => v)

    /** Create a view that counts the number of events that have been emitted.
      *
      * @return The view that will change to contain the latest value emitted by the source
      */
    @inline
    final def eventCount: View[Long] = {
      new View[Long] {
        val data = new AtomicLong(0L)

        this.disposes(self.subscribe { _ =>
          data.incrementAndGet()
          invalidations.invalidate()
        })

        override def value: Long = data.get()

        override def invalidations: Invalidations = Invalidations()
      }
    }

    /** Update a cell with events using a combining function
      */
    @inline
    final def update[B](cell: Cell[B])(f: (A, B) => B): AutoCloseable = {
      self.subscribe(a => cell.update(b => f(a, b)))
    }

    /** Get a Source that routes a Source via a SinkSource.
      */
    @inline
    final def via[B](that: SinkSource[A, B]): Source[B] = {
      that += self into that
      that
    }

    /** Subscribe the Sink to the Source.
      */
    @inline
    final def tap(that: Sink[A]): Source[A] = {
      self += self into that
      self
    }

    /** Fold the event source into a value given the value's initial state.
      *
      * @param f The folding function
      * @param initial The initial state
      * @tparam B Type of the new value
      * @return The value.
      */
    @inline
    final def foldLeft[B](initial: B)(f: (B, A) => B): View[B] = {
      val cell = Cell[B](initial)

      cell.disposes(self.subscribe(v => cell.update(a => f(a, v))))

      cell
    }

    /** Fold the event source into a cell.
      *
      * @param f The folding function
      * @tparam B Type of the new value
      * @return Subscription, which when closed stops updating the cell
      */
    @inline
    final def foldRightInto[B](cell: Cell[B])(f: (A, => B) => B): Closeable = {
      self.subscribe { a =>
        cell.update(b => f(a, b))
      }
    }

    /** Fold the event source into a cell.
      *
      * @param f The folding function
      * @tparam B Type of the new value
      * @return Subscription, which when closed stops updating the cell
      */
    @inline
    final def foldLeftInto[B](cell: Cell[B])(f: (B, A) => B): Closeable = {
      self.subscribe { a =>
        cell.update(b => f(b, a))
      }
    }

    /** Fold the event source into a cell using Monoid.combine
      *
      * @return The view containing folded value.
      */
    @inline
    final def combined(implicit ev: Monoid[A]): View[A] = {
      self.foldLeft(Monoid[A].empty)(Monoid[A].combine)
    }

    /** Fold the event source using Semigroup.combine into a cell.
      *
      * @return Subscription, which when closed stops updating the cell
      */
    @inline
    final def combinedInto(cell: Cell[A])(implicit ev: Semigroup[A]): Closeable = {
      self.subscribe { a =>
        cell.update(ev.combine(_, a))
      }
    }
  }

  implicit class SourceOps_KhVNHpu[A, B](val self: Source[Either[A, B]]) extends AnyVal {
    /** Divert values on the left of emitted events by the source into the provided sink.
      *
      * Values on the right of the event will be emitted by the returned source.
      *
      * @param sink The sink to which left side of emitted events will be published
      * @return The source that emits the right side of emitted events
      */
    @inline
    final def divertLeft(sink: Sink[A]): Source[B] = {
      new SimpleBus[B] { temp =>
        temp += self.subscribe {
          case Right(rt) => temp.publish(rt)
          case Left(lt) => sink.publish(lt)
        }
      }
    }

    /** Divert values on the right of emitted events by the source into the provided sink.
      *
      * Values on the left of the event will be emitted by the returned source.
      *
      * @param sink The sink to which right side of emitted events will be published
      * @return The source that emits the left side of emitted events
      */
    @inline
    final def divertRight(sink: Sink[B]): Source[A] = {
      new SimpleBus[A] { temp =>
        temp += self.subscribe {
          case Right(rt) => sink.publish(rt)
          case Left(lt) => temp.publish(lt)
        }
      }
    }

    /** New Source propagating left of Either.
      */
    @inline
    final def justLeft: Source[A] = divertRight(ClosedSink)

    /** New Source propagating right of Either.
      */
    @inline
    final def justRight: Source[B] = divertLeft(ClosedSink)
  }

  implicit class SourceOps_iY4kPqc[A](val self: Source[Future[A]]) extends AnyVal {
    /** Return a source which emits events whenever the futures from the original
      * source complete.  Success values will be emitted on the right and failures
      * will be emitted on the left.
      */
    @inline
    final def completed(implicit ec: ExecutionContext): Source[Either[Throwable, A]] = {
      val bus = Bus[Either[Throwable, A]]

      bus.disposes(self.subscribe(_.completeInto(bus)))

      bus
    }
  }

  implicit class SourceOps_ZWi7qoi[A](val self: Source[Option[A]]) extends AnyVal {
    /** Propagate Some value as Right and None as the provided Left value.
      */
    @inline
    final def right[B](leftValue: => B): Source[Either[B, A]] = self.map {
      case Some(v) => Right(v)
      case None => Left(leftValue)
    }

    /** Propagate Some value as Left and None as the provided Right value.
      */
    @inline
    final def left[B](rightValue: => B): Source[Either[A, B]] = self.map {
      case Some(v) => Left(v)
      case None => Right(rightValue)
    }

    /** Propagate Some value.
      */
    def justSome: Source[A] = self.mapConcat[Option, A](identity)
  }

  implicit class SourceOps_r8pPKQJ(val self: Source[Long]) extends AnyVal {
    @inline
    final def sum: View[Long] = {
      val cell = LongCell(0L)

      cell.disposes(self.subscribe(cell.addAndGet(_)))

      cell
    }
  }

  implicit class SourceOps_iAPaWug(val self: Source[Int]) extends AnyVal {
    @inline
    final def sum: View[Int] = {
      val cell = IntCell(0)

      cell.disposes(self.subscribe(cell.addAndGet(_)))

      cell
    }
  }
}
