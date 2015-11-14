/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdklib.internal.repository.packages;

import junit.framework.TestCase;

public class FullRevisionTest extends TestCase {

    public final void testFullRevision() {
        FullRevision p = new FullRevision(5);
        assertEquals(5, p.getMajor());
        assertEquals(FullRevision.IMPLICIT_MINOR_REV, p.getMinor());
        assertEquals(FullRevision.IMPLICIT_MICRO_REV, p.getMicro());
        assertEquals(FullRevision.NOT_A_PREVIEW, p.getPreview());
        assertFalse (p.isPreview());
        assertEquals("5", p.toShortString());
        assertEquals(p, FullRevision.parseRevision("5"));
        assertEquals("5.0.0", p.toString());
        assertEquals(p, FullRevision.parseRevision("5.0.0"));

        p = new FullRevision(5, 0, 0, 6);
        assertEquals(5, p.getMajor());
        assertEquals(FullRevision.IMPLICIT_MINOR_REV, p.getMinor());
        assertEquals(FullRevision.IMPLICIT_MICRO_REV, p.getMicro());
        assertEquals(6, p.getPreview());
        assertTrue  (p.isPreview());
        assertEquals("5 rc6", p.toShortString());
        assertEquals(p, FullRevision.parseRevision("5 rc6"));
        assertEquals("5.0.0 rc6", p.toString());
        assertEquals(p, FullRevision.parseRevision("5.0.0 rc6"));

        p = new FullRevision(6, 7, 0);
        assertEquals(6, p.getMajor());
        assertEquals(7, p.getMinor());
        assertEquals(0, p.getMicro());
        assertEquals(0, p.getPreview());
        assertFalse (p.isPreview());
        assertEquals("6.7", p.toShortString());
        assertEquals(p, FullRevision.parseRevision("6.7"));
        assertEquals("6.7.0", p.toString());
        assertEquals(p, FullRevision.parseRevision("6.7.0"));

        p = new FullRevision(10, 11, 12, FullRevision.NOT_A_PREVIEW);
        assertEquals(10, p.getMajor());
        assertEquals(11, p.getMinor());
        assertEquals(12, p.getMicro());
        assertEquals(0, p.getPreview());
        assertFalse (p.isPreview());
        assertEquals("10.11.12", p.toShortString());
        assertEquals("10.11.12", p.toString());
        assertEquals(p, FullRevision.parseRevision("10.11.12"));

        p = new FullRevision(10, 11, 12, 13);
        assertEquals(10, p.getMajor());
        assertEquals(11, p.getMinor());
        assertEquals(12, p.getMicro());
        assertEquals(13, p.getPreview());
        assertTrue  (p.isPreview());
        assertEquals("10.11.12 rc13", p.toShortString());
        assertEquals("10.11.12 rc13", p.toString());
        assertEquals(p, FullRevision.parseRevision("10.11.12 rc13"));
        assertEquals(p, FullRevision.parseRevision("   10.11.12 rc13"));
        assertEquals(p, FullRevision.parseRevision("10.11.12 rc13   "));
        assertEquals(p, FullRevision.parseRevision("   10.11.12   rc13   "));
    }

    public final void testParseError() {
        String errorMsg = null;
        try {
            FullRevision.parseRevision("not a number");
            fail("FullRevision.parseRevision should thrown NumberFormatException");
        } catch (NumberFormatException e) {
            errorMsg = e.getMessage();
        }
        assertEquals("Invalid full revision: not a number", errorMsg);

        errorMsg = null;
        try {
            FullRevision.parseRevision("5 .6 .7");
            fail("FullRevision.parseRevision should thrown NumberFormatException");
        } catch (NumberFormatException e) {
            errorMsg = e.getMessage();
        }
        assertEquals("Invalid full revision: 5 .6 .7", errorMsg);

        errorMsg = null;
        try {
            FullRevision.parseRevision("5.0.0 preview 1");
            fail("FullRevision.parseRevision should thrown NumberFormatException");
        } catch (NumberFormatException e) {
            errorMsg = e.getMessage();
        }
        assertEquals("Invalid full revision: 5.0.0 preview 1", errorMsg);

        errorMsg = null;
        try {
            FullRevision.parseRevision("  5.1.2 rc 42  ");
            fail("FullRevision.parseRevision should thrown NumberFormatException");
        } catch (NumberFormatException e) {
            errorMsg = e.getMessage();
        }
        assertEquals("Invalid full revision:   5.1.2 rc 42  ", errorMsg);
    }

    public final void testCompareTo() {
        FullRevision s4 = new FullRevision(4);
        FullRevision i4 = new FullRevision(4);
        FullRevision g5 = new FullRevision(5, 1, 0, 6);
        FullRevision y5 = new FullRevision(5);
        FullRevision c5 = new FullRevision(5, 1, 0, 6);
        FullRevision o5 = new FullRevision(5, 0, 0, 7);
        FullRevision p5 = new FullRevision(5, 1, 0, 0);

        assertEquals(s4, i4);                   // 4.0.0-0 == 4.0.0-0
        assertEquals(g5, c5);                   // 5.1.0-6 == 5.1.0-6

        assertFalse(y5.equals(p5));             // 5.0.0-0 != 5.1.0-0
        assertFalse(g5.equals(p5));             // 5.1.0-6 != 5.1.0-0
        assertTrue (s4.compareTo(i4) == 0);     // 4.0.0-0 == 4.0.0-0
        assertTrue (s4.compareTo(y5)  < 0);     // 4.0.0-0  < 5.0.0-0
        assertTrue (y5.compareTo(y5) == 0);     // 5.0.0-0 == 5.0.0-0
        assertTrue (y5.compareTo(p5)  < 0);     // 5.0.0-0  < 5.1.0-0
        assertTrue (o5.compareTo(y5)  < 0);     // 5.0.0-7  < 5.0.0-0
        assertTrue (p5.compareTo(p5) == 0);     // 5.1.0-0 == 5.1.0-0
        assertTrue (c5.compareTo(p5)  < 0);     // 5.1.0-6  < 5.1.0-0
        assertTrue (p5.compareTo(c5)  > 0);     // 5.1.0-0  > 5.1.0-6
        assertTrue (p5.compareTo(o5)  > 0);     // 5.1.0-0  > 5.0.0-7
        assertTrue (c5.compareTo(o5)  > 0);     // 5.1.0-6  > 5.0.0-7
        assertTrue (o5.compareTo(o5) == 0);     // 5.0.0-7  > 5.0.0-7
    }

}
