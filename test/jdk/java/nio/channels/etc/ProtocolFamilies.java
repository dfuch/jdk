/*
 * Copyright (c) 2020, 2026, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.NetworkConfiguration;
import jdk.test.lib.net.IPSupport;

import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;
import java.nio.channels.spi.SelectorProvider;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.out;
import static java.net.StandardProtocolFamily.INET;
import static java.net.StandardProtocolFamily.INET6;
import static jdk.test.lib.net.IPSupport.*;

import jtreg.SkippedException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/*
 * @test
 * @summary Test SocketChannel, ServerSocketChannel and DatagramChannel
 *          with various ProtocolFamily combinations
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 * @run junit ProtocolFamilies
 * @run junit/othervm -Djava.net.preferIPv4Stack=true ProtocolFamilies
 */


public class ProtocolFamilies {
    static final boolean hasIPv6 = hasIPv6();
    static final boolean preferIPv4 = preferIPv4Stack();
    static Inet4Address ia4;
    static Inet6Address ia6;

    @BeforeAll()
    public static void setup() throws Exception {
        NetworkConfiguration.printSystemConfiguration(out);
        IPSupport.printPlatformSupport(out);
        try {
            throwSkippedExceptionIfNonOperational();
        } catch (SkippedException skippedException) {
            // jtreg.SkippedException would cause a JUnit test to fail
            Assumptions.assumeTrue(false, skippedException.getMessage());
        }

        ia4 = getLocalIPv4Address();
        ia6 = getLocalIPv6Address();
        out.println("ia4: " + ia4);
        out.println("ia6: " + ia6 + "\n");
    }

    static final Class<UnsupportedAddressTypeException> UATE = UnsupportedAddressTypeException.class;
    static final Class<UnsupportedOperationException> UOE = UnsupportedOperationException.class;

    @FunctionalInterface
    interface ChannelFactory<T extends Closeable> {
        T open() throws IOException;
    }

    /**
     * If {@code expectedException} is non-null, asserts that the {@link ChannelFactory#open()}
     * method throws the expected exception. Otherwise, asserts that the
     * {@link ChannelFactory#open()} method does not throw.
     * This method ensure that the closeable object returned by the {@link ChannelFactory#open()}
     * method is always closed if {@code open} does not throw.
     * @param expectedException the expected exception, or {@code null} if no exception is
     *                          expected
     * @param factory a factory method that produces a {@link Closeable} object
     * @param <T> the concrete object type created by the factory
     */
    private static <T extends Closeable> void assertOpenAndClose(Class<? extends Throwable> expectedException,
                                                                 ChannelFactory<T> factory)
    {
        final AtomicReference<Closeable> sc = new AtomicReference<>();
        try {
            if (expectedException != null) {
                assertThrows(expectedException, () -> sc.set(factory.open()));
            } else {
                assertDoesNotThrow(() -> sc.set(factory.open()));
            }
        } catch (Throwable t) {
            // test failed, ensure sc is closed
            try {
                close(sc.get());
            } catch (Throwable cx) {
                t.addSuppressed(cx);
            }
            // propagate the original failure
            throw t;
        }
        // test passed, ensure sc is closed
        assertDoesNotThrow(() -> close(sc.get()));
    }


    private static void close(Closeable sc) throws IOException {
        if (sc != null) sc.close();
    }



    public static List<Arguments> open() {
        if (hasIPv6 && !preferIPv4) {
            return List.of(
                    Arguments.of(  INET,   null  ),
                    Arguments.of(  INET6,  null  )
            );
        } else {
            return List.of(
                    Arguments.of(  INET,   null  ),
                    Arguments.of(  INET6,  UOE   )
            );
        }
    }

    @ParameterizedTest
    @MethodSource("open")
    public void scOpen(StandardProtocolFamily family,
                       Class<? extends Exception> expectedException)
    {
        assertOpenAndClose(expectedException, () -> openSC(family));
    }

    @ParameterizedTest
    @MethodSource("open")
    public void sscOpen(StandardProtocolFamily family,
                        Class<? extends Exception> expectedException)
    {
        assertOpenAndClose(expectedException, () -> openSSC(family));
    }

    @ParameterizedTest
    @MethodSource("open")
    public void dcOpen(StandardProtocolFamily family,
                       Class<? extends Exception> expectedException)
    {
        assertOpenAndClose(expectedException, () -> openDC(family));
    }

    public static List<Arguments> openBind() {
        if (hasIPv6 && !preferIPv4) {
            return List.of(
                    Arguments.of(  INET,   INET,   null   ),
                    Arguments.of(  INET,   INET6,  UATE   ),
                    Arguments.of(  INET,   null,   null   ),
                    Arguments.of(  INET6,  INET,   null   ),
                    Arguments.of(  INET6,  INET6,  null   ),
                    Arguments.of(  INET6,  null,   null   ),
                    Arguments.of(  null,   INET,   null   ),
                    Arguments.of(  null,   INET6,  null   ),
                    Arguments.of(  null,   null,   null   )
            );
        } else {
            return List.of(
                    Arguments.of(  INET,   INET,   null   ),
                    Arguments.of(  INET,   INET6,  UATE   ),
                    Arguments.of(  INET,   null,   null   ),
                    Arguments.of(  null,   INET,   null   ),
                    Arguments.of(  null,   INET6,  UATE   ),
                    Arguments.of(  null,   null,   null   )
            );
        }
    }

    // SocketChannel open - INET, INET6, default
    // SocketChannel bind - INET, INET6, null

    @ParameterizedTest
    @MethodSource("openBind")
    public void scOpenBind(StandardProtocolFamily ofamily,
                           StandardProtocolFamily bfamily,
                           Class<? extends Exception> expectedException)
        throws Throwable
    {
        try (SocketChannel sc = openSC(ofamily)) {
            SocketAddress addr = getSocketAddress(bfamily);
            Executable bindOp = () -> sc.bind(addr);
            if (expectedException == null)
                bindOp.execute();
            else
                assertThrows(expectedException, bindOp);
        }
    }

    //  ServerSocketChannel open - INET, INET6, default
    //  ServerSocketChannel bind - INET, INET6, null

    @ParameterizedTest
    @MethodSource("openBind")
    public void sscOpenBind(StandardProtocolFamily ofamily,
                            StandardProtocolFamily bfamily,
                            Class<? extends Exception> expectedException)
        throws Throwable
    {
        try (ServerSocketChannel ssc = openSSC(ofamily)) {
            SocketAddress addr = getSocketAddress(bfamily);
            Executable bindOp = () -> ssc.bind(addr);
            if (expectedException == null)
                bindOp.execute();
            else
                assertThrows(expectedException, bindOp);
        }
    }

    //  DatagramChannel open - INET, INET6, default
    //  DatagramChannel bind - INET, INET6, null

    @ParameterizedTest
    @MethodSource("openBind")
    public void dcOpenBind(StandardProtocolFamily ofamily,
                           StandardProtocolFamily bfamily,
                           Class<? extends Exception> expectedException)
        throws Throwable
    {
        try (DatagramChannel dc = openDC(ofamily)) {
            SocketAddress addr = getSocketAddress(bfamily);
            Executable bindOp = () -> dc.bind(addr);
            if (expectedException == null)
                bindOp.execute();
            else
                assertThrows(expectedException, bindOp);
        }
    }

    //  SocketChannel open    - INET, INET6, default
    //  SocketChannel connect - INET, INET6, default

    public static List<Arguments> openConnect() {
        if (hasIPv6 && !preferIPv4) {
            return List.of(
                    Arguments.of(  INET,   INET,   null   ),
                    Arguments.of(  INET,   INET6,  null   ),
                    Arguments.of(  INET,   null,   null   ),
                    Arguments.of(  INET6,  INET,   UATE   ),
                    Arguments.of(  INET6,  INET6,  null   ),
                    Arguments.of(  INET6,  null,   null   ),
                    Arguments.of(  null,   INET,   UATE   ),
                    Arguments.of(  null,   INET6,  null   ),
                    Arguments.of(  null,   null,   null   )
            );
        } else {
            // INET6 channels cannot be created - UOE - tested elsewhere
            return List.of(
                    Arguments.of(  INET,   INET,   null   ),
                    Arguments.of(  INET,   null,   null   ),
                    Arguments.of(  null,   INET,   null   ),
                    Arguments.of(  null,   null,   null   )
            );
        }
    }

    @ParameterizedTest
    @MethodSource("openConnect")
    public void scOpenConnect(StandardProtocolFamily sfamily,
                              StandardProtocolFamily cfamily,
                              Class<? extends Exception> expectedException)
        throws Throwable
    {
        try (ServerSocketChannel ssc = openSSC(sfamily)) {
            ssc.bind(null);
            SocketAddress saddr = ssc.getLocalAddress();
            try (SocketChannel sc = openSC(cfamily)) {
                if (expectedException == null)
                    sc.connect(saddr);
                else
                    assertThrows(expectedException, () -> sc.connect(saddr));
            }
        }
    }

    static final Class<NullPointerException> NPE = NullPointerException.class;

    // Tests null handling
    @Test
    public void testNulls() {
        assertOpenAndClose(NPE, () -> SocketChannel.open((ProtocolFamily) null));
        assertOpenAndClose(NPE, () -> ServerSocketChannel.open(null));
        assertOpenAndClose(NPE, () -> DatagramChannel.open(null));

        assertOpenAndClose(NPE, () -> SelectorProvider.provider().openSocketChannel(null));
        assertOpenAndClose(NPE, () -> SelectorProvider.provider().openServerSocketChannel(null));
        assertOpenAndClose(NPE, () -> SelectorProvider.provider().openDatagramChannel(null));
    }

    static final ProtocolFamily BAD_PF = () -> "BAD_PROTOCOL_FAMILY";

    // Tests UOE handling
    @Test
    public void testUoe() {
        assertOpenAndClose(UOE, () -> SocketChannel.open(BAD_PF));
        assertOpenAndClose(UOE, () -> ServerSocketChannel.open(BAD_PF));
        assertOpenAndClose(UOE, () -> DatagramChannel.open(BAD_PF));

        assertOpenAndClose(UOE, () -> SelectorProvider.provider().openSocketChannel(BAD_PF));
        assertOpenAndClose(UOE, () -> SelectorProvider.provider().openServerSocketChannel(BAD_PF));
        assertOpenAndClose(UOE, () -> SelectorProvider.provider().openDatagramChannel(BAD_PF));
    }

    // Helper methods

    private static SocketChannel openSC(StandardProtocolFamily family)
            throws IOException {
        SocketChannel sc = family == null ? SocketChannel.open()
                : SocketChannel.open(family);
        return sc;
    }

    private static ServerSocketChannel openSSC(StandardProtocolFamily family)
            throws IOException {
        ServerSocketChannel ssc = family == null ? ServerSocketChannel.open()
                : ServerSocketChannel.open(family);
        return ssc;
    }

    private static DatagramChannel openDC(StandardProtocolFamily family)
            throws IOException {
        DatagramChannel dc = family == null ? DatagramChannel.open()
                : DatagramChannel.open(family);
        return dc;
    }

    private static SocketAddress getSocketAddress(StandardProtocolFamily family) {
        return family == null ? null : switch (family) {
            case INET -> new InetSocketAddress(ia4, 0);
            case INET6 -> new InetSocketAddress(ia6, 0);
            default -> throw new RuntimeException("Unexpected protocol family");
        };
    }

    private static SocketAddress getLoopback(StandardProtocolFamily family, int port)
            throws UnknownHostException {
        if ((family == null || family == INET6) && hasIPv6) {
            return new InetSocketAddress(InetAddress.getByName("::1"), port);
        } else {
            return new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port);
        }
    }

    private static Inet4Address getLocalIPv4Address()
            throws Exception {
        return NetworkConfiguration.probe()
                .ip4Addresses()
                .filter(a -> !a.isLoopbackAddress())
                .findFirst()
                .orElse((Inet4Address)InetAddress.getByName("0.0.0.0"));
    }

    private static Inet6Address getLocalIPv6Address()
            throws Exception {
        return NetworkConfiguration.probe()
                .ip6Addresses()
                .filter(a -> !a.isLoopbackAddress())
                .findFirst()
                .orElse((Inet6Address) InetAddress.getByName("::0"));
    }
}
