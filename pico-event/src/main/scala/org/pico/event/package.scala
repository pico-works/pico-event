package org.pico

package object event {
  /** A Bus is a SinkSource where the Sink event type is the same as the Source event type.
    */
  type Bus[A] = SinkSource[A, A]
}
