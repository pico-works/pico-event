package org.pico.event

import java.io.Closeable
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import org.pico.atomic.syntax.std.atomicReference._
import org.pico.disposal.OnClose

trait BareSinkSource[A, B] extends SinkSource[A, B] {
  private final val subscribers = new AtomicReference(List.empty[WeakReference[B => Unit]])
  private final val garbage = new AtomicInteger(0)

  def transform: A => B

  override def subscribe(subscriber: (B) => Unit): Closeable = {
    val subscriberRef = new WeakReference(subscriber)

    subscribers.update(subscriberRef :: _)

    houseKeep()

    OnClose {
      identity(subscriber)
      subscriberRef.clear()
      houseKeep()
    }
  }

  override def publish(event: A): Unit = {
    val v = transform(event)

    subscribers.get().foreach { subscriberRef =>
      val subscriber = subscriberRef.get()

      if (subscriber != null) {
        subscriber(v)
      } else {
        garbage.incrementAndGet()
      }
    }

    houseKeep()
  }

  final def houseKeep(): Unit = {
    if (garbage.get() > subscribers.get().size) {
      garbage.set(0)
      subscribers.update { subscriptions =>
        subscriptions.filter { subscription =>
          subscription.get() != null
        }
      }
    }
  }
}

object BareSinkSource {
  def apply[A, B](f: A => B): SinkSource[A, B] = {
    new BareSinkSource[A, B] {
      override def transform: A => B = f
    }
  }
}