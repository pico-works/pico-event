package org.pico.event

import java.io.Closeable
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import org.pico.atomic.syntax.std.atomicReference._
import org.pico.disposal.{OnClose, SimpleDisposer}

/** A simple SinkSource which implements subscriber tracking.
  *
  * This implementation does not release references have a well-defined closed state.
  * The SimpleSinkSource wrapper type will properly define the closed state.
  *
  * @tparam A The sink event type
  * @tparam B The source event type
  */
trait Subscribers[-A, +B] extends SimpleDisposer {
  def subscribe(subscriber: (B) => Unit): Closeable

  def publish(event: A): Unit

  def houseKeep(): Unit
}

object Subscribers {
  def apply[A, B](f: A => B): Subscribers[A, B] = {
    new Subscribers[A, B] {
      val subscribers = new AtomicReference(List.empty[WeakReference[Wrapper[B => Unit]]])
      val garbage = new AtomicInteger(0)

      def transform: A => B = f

      override def subscribe(subscriber: (B) => Unit): Closeable = {
        val wrapper = Wrapper(subscriber)
        val subscriberRef = new WeakReference(wrapper)

        subscribers.update(subscriberRef :: _)

        houseKeep()

        OnClose {
          identity(wrapper)
          subscriberRef.clear()
          houseKeep()
        }
      }

      override def publish(event: A): Unit = {
        val v = transform(event)

        subscribers.get().foreach { subscriberRef =>
          var wrapper = subscriberRef.get()

          if (wrapper != null) {
            val subscriber = wrapper.target
            // Drop reference to wrapper so that the garbage collector can collect it if there are no
            // other references to it.  This helps facilitate earlier collection, especially if a lot
            // of time is spent in the subscriber.  This is why wrapper is a var.
            wrapper = null
            subscriber(v)
          } else {
            garbage.incrementAndGet()
          }
        }

        houseKeep()
      }

      override def houseKeep(): Unit = {
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
  }
}
