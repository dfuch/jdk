/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates. All rights reserved.
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

/*
 * @test
 * @bug 8318671
 * @summary Tests various ways to call memstat
 * @library /test/lib /
 *
 * @run driver compiler.compilercontrol.commands.MemStatTest
 */

package compiler.compilercontrol.commands;

import jdk.test.lib.process.ProcessTools;

public class MemStatTest {
    public static void main(String[] args) throws Exception {
        // default => collect
        ProcessTools.executeTestJava("-XX:CompileCommand=MemStat,*.*", "-version")
                .shouldHaveExitValue(0)
                .shouldNotContain("CompileCommand: An error occurred during parsing")
                .shouldContain("CompileCommand: MemStat *.* uintx MemStat = 1"); // should be registered
        // collect explicit
        ProcessTools.executeTestJava("-XX:CompileCommand=MemStat,*.*,collect", "-version")
                .shouldHaveExitValue(0)
                .shouldNotContain("CompileCommand: An error occurred during parsing")
                .shouldContain("CompileCommand: MemStat *.* uintx MemStat = 1"); // should be registered
        // print explicit
        ProcessTools.executeTestJava("-XX:CompileCommand=MemStat,*.*,print", "-version")
                .shouldHaveExitValue(0)
                .shouldNotContain("CompileCommand: An error occurred during parsing")
                .shouldContain("CompileCommand: MemStat *.* uintx MemStat = 2");
        // invalid suboption
        ProcessTools.executeTestJava("-XX:CompileCommand=MemStat,*.*,invalid", "-version")
                .shouldNotHaveExitValue(0)
                .shouldContain("CompileCommand: An error occurred during parsing")
                .shouldContain("Error: Value cannot be read for option 'MemStat'")
                .shouldNotContain("CompileCommand: MemStat"); // should *NOT* be registered
    }
}
