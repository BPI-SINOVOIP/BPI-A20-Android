/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.primitives;

import com.google.common.collect.testing.Helpers;
import com.google.common.testing.NullPointerTester;
import com.google.common.testing.SerializableTester;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Unit test for {@link UnsignedBytes}.
 *
 * @author Kevin Bourrillion
 */
public class UnsignedBytesTest extends TestCase {
  private static final byte LEAST = 0;
  private static final byte GREATEST = (byte) 255;

  // Only in this class, VALUES must be strictly ascending
  private static final byte[] VALUES =
      {LEAST, 127, (byte) 128, (byte) 129, GREATEST};

  public void testToInt() {
    assertEquals(0, UnsignedBytes.toInt((byte) 0));
    assertEquals(1, UnsignedBytes.toInt((byte) 1));
    assertEquals(127, UnsignedBytes.toInt((byte) 127));
    assertEquals(128, UnsignedBytes.toInt((byte) -128));
    assertEquals(129, UnsignedBytes.toInt((byte) -127));
    assertEquals(255, UnsignedBytes.toInt((byte) -1));
  }

  public void testCheckedCast() {
    for (byte value : VALUES) {
      assertEquals(value,
          UnsignedBytes.checkedCast(UnsignedBytes.toInt(value)));
    }
    assertCastFails(256L);
    assertCastFails(-1L);
    assertCastFails(Long.MAX_VALUE);
    assertCastFails(Long.MIN_VALUE);
  }

  public void testSaturatedCast() {
    for (byte value : VALUES) {
      assertEquals(value,
          UnsignedBytes.saturatedCast(UnsignedBytes.toInt(value)));
    }
    assertEquals(GREATEST, UnsignedBytes.saturatedCast(256L));
    assertEquals(LEAST, UnsignedBytes.saturatedCast(-1L));
    assertEquals(GREATEST, UnsignedBytes.saturatedCast(Long.MAX_VALUE));
    assertEquals(LEAST, UnsignedBytes.saturatedCast(Long.MIN_VALUE));
  }

  private void assertCastFails(long value) {
    try {
      UnsignedBytes.checkedCast(value);
      fail("Cast to byte should have failed: " + value);
    } catch (IllegalArgumentException ex) {
      assertTrue(value + " not found in exception text: " + ex.getMessage(),
          ex.getMessage().contains(String.valueOf(value)));
    }
  }

  public void testCompare() {
    // This is the only ordering for primitives that does not have a
    // corresponding Comparable wrapper in java.lang.
    for (int i = 0; i < VALUES.length; i++) {
      for (int j = 0; j < VALUES.length; j++) {
        byte x = VALUES[i];
        byte y = VALUES[j];
        // note: spec requires only that the sign is the same
        assertEquals(x + ", " + y,
                     Math.signum(UnsignedBytes.compare(x, y)),
                     Math.signum(Ints.compare(i, j)));
      }
    }
  }

  public void testMax_noArgs() {
    try {
      UnsignedBytes.max();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMax() {
    assertEquals(LEAST, UnsignedBytes.max(LEAST));
    assertEquals(GREATEST, UnsignedBytes.max(GREATEST));
    assertEquals((byte) 255, UnsignedBytes.max(
        (byte) 0, (byte) -128, (byte) -1, (byte) 127, (byte) 1));
  }

  public void testMin_noArgs() {
    try {
      UnsignedBytes.min();
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  public void testMin() {
    assertEquals(LEAST, UnsignedBytes.min(LEAST));
    assertEquals(GREATEST, UnsignedBytes.min(GREATEST));
    assertEquals((byte) 0, UnsignedBytes.min(
        (byte) 0, (byte) -128, (byte) -1, (byte) 127, (byte) 1));
  }

  public void testJoin() {
    assertEquals("", UnsignedBytes.join(",", new byte[] {}));
    assertEquals("1", UnsignedBytes.join(",", new byte[] {(byte) 1}));
    assertEquals("1,2", UnsignedBytes.join(",", (byte) 1, (byte) 2));
    assertEquals("123", UnsignedBytes.join("", (byte) 1, (byte) 2, (byte) 3));
    assertEquals("128,255", UnsignedBytes.join(",", (byte) 128, (byte) -1));
  }

  public void testLexicographicalComparatorDefaultChoice() {
    Comparator<byte[]> defaultComparator =
        UnsignedBytes.lexicographicalComparator();
    Comparator<byte[]> unsafeComparator =
        UnsignedBytes.LexicographicalComparatorHolder.UnsafeComparator.INSTANCE;
    assertSame(defaultComparator, unsafeComparator);
  }

  public void testLexicographicalComparator() {
    List<byte[]> ordered = Arrays.asList(
        new byte[] {},
        new byte[] {LEAST},
        new byte[] {LEAST, LEAST},
        new byte[] {LEAST, (byte) 1},
        new byte[] {(byte) 1},
        new byte[] {(byte) 1, LEAST},
        new byte[] {GREATEST, GREATEST - (byte) 1},
        new byte[] {GREATEST, GREATEST},
        new byte[] {GREATEST, GREATEST, GREATEST});

    // The Unsafe implementation if it's available. Otherwise, the Java implementation.
    Comparator<byte[]> comparator = UnsignedBytes.lexicographicalComparator();
    Helpers.testComparator(comparator, ordered);
    assertSame(comparator, SerializableTester.reserialize(comparator));

    // The Java implementation.
    Comparator<byte[]> javaImpl = UnsignedBytes.lexicographicalComparatorJavaImpl();
    Helpers.testComparator(javaImpl, ordered);
    assertSame(javaImpl, SerializableTester.reserialize(javaImpl));
  }

  public void testNulls() throws Exception {
    NullPointerTester tester = new NullPointerTester();
    tester.setDefault(byte[].class, new byte[0]);
    tester.testAllPublicStaticMethods(UnsignedBytes.class);
  }
}
