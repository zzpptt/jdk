/*
 * Copyright 2006 Sun Microsystems, Inc.  All Rights Reserved.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/**
 * @test
 * @bug 4225317
 * @summary Check extracted files have date as per those in the .jar file
 */

import java.io.File;
import java.io.PrintWriter;
import java.util.Date;
import sun.tools.jar.Main;

public class JarEntryTime {
    static boolean cleanup(File dir) throws Throwable {
        boolean rc = true;
        File[] x = dir.listFiles();
        if (x != null) {
            for (int i = 0; i < x.length; i++) {
                rc &= x[i].delete();
            }
        }
        return rc & dir.delete();
    }

    static void extractJar(File jarFile, boolean useExtractionTime) throws Throwable {
        String javahome = System.getProperty("java.home");
        if (javahome.endsWith("jre")) {
            javahome = javahome.substring(0, javahome.length() - 4);
        }
        String jarcmd = javahome + File.separator + "bin" + File.separator + "jar";
        String[] args;
        if (useExtractionTime) {
            args = new String[] {
                jarcmd,
                "-J-Dsun.tools.jar.useExtractionTime=true",
                "xf",
                jarFile.getName() };
        } else {
            args = new String[] {
                jarcmd,
                "xf",
                jarFile.getName() };
        }
        Process p = Runtime.getRuntime().exec(args);
        check(p != null && (p.waitFor() == 0));
    }

    public static void realMain(String[] args) throws Throwable {
        final long now = System.currentTimeMillis();
        final long earlier = now - (60L * 60L * 6L * 1000L);
        final long yesterday = now - (60L * 60L * 24L * 1000L);

        // ZipEntry's mod date has 2 seconds precision: give extra time to
        // allow for e.g. rounding/truncation and networked/samba drives.
        final long PRECISION = 10000L;

        File dirOuter = new File("outer");
        File dirInner = new File(dirOuter, "inner");

        File jarFile = new File("JarEntryTime.jar");

        // Remove any leftovers from prior run
        cleanup(dirInner);
        cleanup(dirOuter);
        jarFile.delete();

        /* Create a directory structure
         * outer/
         *     inner/
         *         foo.txt
         * Set the lastModified dates so that outer is created now, inner
         * yesterday, and foo.txt created "earlier".
         */
        check(dirOuter.mkdir());
        check(dirInner.mkdir());
        File fileInner = new File(dirInner, "foo.txt");
        PrintWriter pw = new PrintWriter(fileInner);
        pw.println("hello, world");
        pw.close();
        dirOuter.setLastModified(now);
        dirInner.setLastModified(yesterday);
        fileInner.setLastModified(earlier);

        // Make a jar file from that directory structure
        Main jartool = new Main(System.out, System.err, "jar");
        check(jartool.run(new String[] {
                "cf",
                jarFile.getName(), dirOuter.getName() } ));
        check(jarFile.exists());

        check(cleanup(dirInner));
        check(cleanup(dirOuter));

        // Extract and check that the last modified values are those specified
        // in the archive
        extractJar(jarFile, false);
        check(dirOuter.exists());
        check(dirInner.exists());
        check(fileInner.exists());
        check(Math.abs(dirOuter.lastModified() - now) <= PRECISION);
        check(Math.abs(dirInner.lastModified() - yesterday) <= PRECISION);
        check(Math.abs(fileInner.lastModified() - earlier) <= PRECISION);

        check(cleanup(dirInner));
        check(cleanup(dirOuter));

        // Extract and check the last modified values are the current times.
        // See sun.tools.jar.Main
        extractJar(jarFile, true);
        check(dirOuter.exists());
        check(dirInner.exists());
        check(fileInner.exists());
        check(Math.abs(dirOuter.lastModified() - now) <= PRECISION);
        check(Math.abs(dirInner.lastModified() - now) <= PRECISION);
        check(Math.abs(fileInner.lastModified() - now) <= PRECISION);

        check(cleanup(dirInner));
        check(cleanup(dirOuter));

        check(jarFile.delete());
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
