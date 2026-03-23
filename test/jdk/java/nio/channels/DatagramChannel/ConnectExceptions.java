/*
 * Copyright (c) 2018, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
 * @bug 8198753
 * @summary Test DatagramChannel connect exceptions
 * @library ..
 * @run junit ConnectExceptions
 */


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AlreadyConnectedException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.UnresolvedAddressException;
import java.nio.channels.UnsupportedAddressTypeException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConnectExceptions {
    static DatagramChannel sndChannel;
    static DatagramChannel rcvChannel;
    static InetSocketAddress sender;
    static InetSocketAddress receiver;

    @BeforeAll
    public static void setup() throws Exception {
        sndChannel = DatagramChannel.open();
        sndChannel.bind(null);
        InetAddress address = InetAddress.getLocalHost();
        if (address.isLoopbackAddress()) {
            address = InetAddress.getLoopbackAddress();
        }
        sender = new InetSocketAddress(address,
            sndChannel.socket().getLocalPort());

        rcvChannel = DatagramChannel.open();
        rcvChannel.bind(null);
        receiver = new InetSocketAddress(address,
            rcvChannel.socket().getLocalPort());
    }

    @Test
    public void unsupportedAddressTypeException() {
        assertThrows(UnsupportedAddressTypeException.class, () -> {
            rcvChannel.connect(sender);
            sndChannel.connect(new SocketAddress() {});
        });
    }

    @Test
    public void unresolvedAddressException() {
        assertThrows(UnresolvedAddressException.class, () -> {
            String host = TestUtil.UNRESOLVABLE_HOST;
            InetSocketAddress unresolvable = new InetSocketAddress (host, 37);
            sndChannel.connect(unresolvable);
        });
    }

    @Test
    public void alreadyConnectedException() {
        assertThrows(AlreadyConnectedException.class, () -> {
            sndChannel.connect(receiver);
            InetSocketAddress random = new InetSocketAddress(0);
            sndChannel.connect(random);
        });
    }

    @AfterAll
    public static void cleanup() throws Exception {
        rcvChannel.close();
        sndChannel.close();
    }
}
