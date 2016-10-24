package org.pico.event.syntax

import java.util.concurrent.atomic.AtomicReference

import org.pico.event.{ClosedSink, Sink}

package object sink {
  implicit class SinkOps_cHdm8Nd[A](val self: Sink[A]) extends AnyVal {
    /** Create a new sink that only publishes events that satisfy the predicate f.
      *
      * @param f The predicate
      * @return The new filtering sink.
      */
    def cofilter(f: A => Boolean): Sink[A] = Sink[A](a => if (f(a)) self.publish(a))

    /** Create a sink that publishes to one of two sinks depending on the side of the Either.
     */
    def fork[B](that: Sink[B]): Sink[Either[A, B]] = {
      val refSelf = new AtomicReference[Sink[A]](self)
      val refThat = new AtomicReference[Sink[B]](that)

      val sink = Sink[Either[A, B]] {
        case Left(a)  => Option(refSelf.get()).foreach(_.publish(a))
        case Right(b) => Option(refThat.get()).foreach(_.publish(b))
      }

      sink.resets(ClosedSink, refSelf)
      sink.resets(ClosedSink, refThat)

      sink
    }
  }
}
