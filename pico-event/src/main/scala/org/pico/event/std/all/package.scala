package org.pico.event.std

import org.pico.event.HasForeach

package object all {
  implicit val hasForEach_Option_Nsz7ia9 = new HasForeach[Option] {
    override def foreach[A](self: Option[A])(f: (A) => Unit): Unit = self.foreach(f)
  }

  implicit val hasForEach_List_Nsz7ia9 = new HasForeach[List] {
    override def foreach[A](self: List[A])(f: (A) => Unit): Unit = self.foreach(f)
  }

  implicit val hasForEach_Iterable_Nsz7ia9 = new HasForeach[Iterable] {
    override def foreach[A](self: Iterable[A])(f: (A) => Unit): Unit = self.foreach(f)
  }

  implicit val hasForEach_Stream_Nsz7ia9 = new HasForeach[Stream] {
    override def foreach[A](self: Stream[A])(f: (A) => Unit): Unit = self.foreach(f)
  }

  implicit val hasForEach_Seq_Nsz7ia9 = new HasForeach[Seq] {
    override def foreach[A](self: Seq[A])(f: (A) => Unit): Unit = self.foreach(f)
  }

  implicit val hasForEach_Vector_Nsz7ia9 = new HasForeach[Vector] {
    override def foreach[A](self: Vector[A])(f: (A) => Unit): Unit = self.foreach(f)
  }
}
