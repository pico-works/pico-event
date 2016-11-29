package org.pico.event.net

import java.net.InetSocketAddress
import java.nio.ByteBuffer

case class UdpEmitFailed(
    address: InetSocketAddress,
    buffer: ByteBuffer,
    sentBytes: Int)
