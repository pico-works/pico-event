package org.pico.event.syntax

import org.pico.event.Sink

package object sink {
  implicit class SinkOps_cHdm8Nd[A](val self: Sink[A]) extends AnyVal {
    /** Create a new sink that only publishes events that satify the predicate f.
      *
      * @param f The predicate
      * @return The new filtering sink.
      */
    def cofilter(f: A => Boolean): Sink[A] = Sink[A](a => if (f(a)) self.publish(a))
  }
}
