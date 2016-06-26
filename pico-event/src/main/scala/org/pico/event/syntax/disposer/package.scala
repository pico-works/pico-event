package org.pico.event.syntax

import org.pico.disposal.Disposer
import org.pico.event.Cell

package object disposer {
  implicit class DisposerOps_dEhxmsY(val self: Disposer) extends AnyVal {
    /** Register an var reference for reset on close.  When the disposer is closed, the
      * replacement value is swapped in.
      *
      * @param replacement The replacement value to use when swapping
      * @param variable The reference to swap
      * @tparam V The type of the value
      * @return The disposable object
      */
    @inline
    final def resets[V](replacement: V, variable: Cell[V]): Cell[V] = {
      self.onClose(variable.value = replacement)
      variable
    }
  }
}
