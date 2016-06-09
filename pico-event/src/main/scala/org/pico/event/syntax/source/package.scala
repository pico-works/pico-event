package org.pico.event.syntax

import org.pico.disposal.std.autoCloseable._
import org.pico.event.{Live, SimpleBus, Sink, Source}

package object source {
  implicit class SourceOps_hJob2ex[A](val self: Source[A]) extends AnyVal {
    def latest(initial: A): Live[A] = self.foldRight(initial)((v, _) => v)
  }

  implicit class SourceOps_KhVNHpu[A, B](val self: Source[Either[A, B]]) extends AnyVal {
    def divertLeft(sink: Sink[A]): Source[B] = {
      new SimpleBus[B] { temp =>
        temp += self.subscribe {
          case Right(rt) => temp.publish(rt)
          case Left(lt) => sink.publish(lt)
        }
      }
    }

    def divertRight(sink: Sink[B]): Source[A] = {
      new SimpleBus[A] { temp =>
        temp += self.subscribe {
          case Right(rt) => sink.publish(rt)
          case Left(lt) => temp.publish(lt)
        }
      }
    }
  }
}
