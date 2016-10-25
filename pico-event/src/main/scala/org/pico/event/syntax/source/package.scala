package org.pico.event.syntax

import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

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
    def latest(initial: A): View[A] = self.foldRight(initial)((v, _) => v)

    /** Create a view that counts the number of events that have been emitted.
      *
      * @return The view that will change to contain the latest value emitted by the source
      */
    def eventCount: View[Long] = {
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
    def update[B](cell: Cell[B])(f: (A, B) => B): AutoCloseable = {
      self.subscribe(a => cell.update(b => f(a, b)))
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
    def divertLeft(sink: Sink[A]): Source[B] = {
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
    def divertRight(sink: Sink[B]): Source[A] = {
      new SimpleBus[A] { temp =>
        temp += self.subscribe {
          case Right(rt) => sink.publish(rt)
          case Left(lt) => temp.publish(lt)
        }
      }
    }

    /** New Source propagating left of Either.
      */
    def justLeft: Source[A] = divertRight(ClosedSink)

    /** New Source propagating right of Either.
      */
    def justRight: Source[B] = divertLeft(ClosedSink)
  }

  implicit class SourceOps_iY4kPqc[A](val self: Source[Future[A]]) extends AnyVal {
    /** Return a source which emits events whenever the futures from the original
      * source complete.  Success values will be emitted on the right and failures
      * will be emitted on the left.
      */
    @deprecated("Use completed instead")
    def scheduled(implicit ec: ExecutionContext): Source[Either[Throwable, A]] = completed

    /** Return a source which emits events whenever the futures from the original
      * source complete.  Success values will be emitted on the right and failures
      * will be emitted on the left.
      */
    def completed(implicit ec: ExecutionContext): Source[Either[Throwable, A]] = {
      val bus = Bus[Either[Throwable, A]]

      bus.disposes(self.subscribe(_.completeInto(bus)))

      bus
    }
  }

  implicit class SourceOps_ZWi7qoi[A](val self: Source[Option[A]]) extends AnyVal {
    /** Propagate Some value as Right and None as the provided Left value.
      */
    def right[B](leftValue: => B): Source[Either[B, A]] = self.map {
      case Some(v) => Right(v)
      case None => Left(leftValue)
    }

    /** Propagate Some value as Left and None as the provided Right value.
      */
    def left[B](rightValue: => B): Source[Either[A, B]] = self.map {
      case Some(v) => Left(v)
      case None => Right(rightValue)
    }

    /** Propagate Some value.
      */
    def justSome: Source[A] = self.mapConcat[Option, A](identity)
  }

  implicit class SourceOps_r8pPKQJ(val self: Source[Long]) extends AnyVal {
    def sum: View[Long] = {
      val cell = LongCell(0L)

      cell.disposes(self.subscribe(cell.addAndGet(_)))

      cell
    }
  }

  implicit class SourceOps_iAPaWug(val self: Source[Int]) extends AnyVal {
    def sum: View[Int] = {
      val cell = IntCell(0)

      cell.disposes(self.subscribe(cell.addAndGet(_)))

      cell
    }
  }
}
