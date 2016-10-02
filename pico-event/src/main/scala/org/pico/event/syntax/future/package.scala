package org.pico.event.syntax

import org.pico.event.Sink

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

package object future {
  implicit class FutureOps_2Qos8tq[A](val self: Future[A]) extends AnyVal {
    def successInto(sink: Sink[A]): Future[A] = {
      self.foreach { value =>
        sink.publish(value)
      }

      self
    }

    def failureInto(sink: Sink[Throwable]): Future[A] = {
      self.onFailure { case value =>
        sink.publish(value)
      }

      self
    }

    def completeInto(successSink: Sink[A], failureSink: Sink[Throwable]): Future[A] = {
      self.successInto(successSink).failureInto(failureSink)
    }

    def completeInto(sink: Sink[Either[Throwable, A]]): Future[A] = {
      self.successInto(sink.comap(Right(_))).failureInto(sink.comap(Left(_)))
    }
  }
}
