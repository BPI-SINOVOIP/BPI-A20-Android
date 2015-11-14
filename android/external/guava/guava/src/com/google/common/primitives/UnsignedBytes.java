/*
 * Copyright (C) 2009 The Guava Authors
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.VisibleForTesting;

// BEGIN android-changed
//import sun.misc.Unsafe;
// END android-changed

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Comparator;

/**
 * Static utility methods pertaining to {@code byte} primitives that interpret
 * values as <i>unsigned</i> (that is, any negative value {@code b} is treated
 * as the positive value {@code 256 + b}). The corresponding methods that treat
 * the values as signed are found in {@link SignedBytes}, and the methods for
 * which signedness is not an issue are in {@link Bytes}.
 *
 * @author Kevin Bourrillion
 * @author Martin Buchholz
 * @author Hiroshi Yamauchi
 * @since 1.0
 */
public final class UnsignedBytes {
  private UnsignedBytes() {}

  /**
   * The largest power of two that can be represented as an unsigned {@code byte}.
   *
   * @since 10.0
   */
  public static final byte MAX_POWER_OF_TWO = (byte) (1 << 7);

  /**
   * Returns the value of the given byte as an integer, when treated as
   * unsigned. That is, returns {@code value + 256} if {@code value} is
   * negative; {@code value} itself otherwise.
   *
   * @since 6.0
   */
  public static int toInt(byte value) {
    return value & 0xFF;
  }

  /**
   * Returns the {@code byte} value that, when treated as unsigned, is equal to
   * {@code value}, if possible.
   *
   * @param value a value between 0 and 255 inclusive
   * @return the {@code byte} value that, when treated as unsigned, equals
   *     {@code value}
   * @throws IllegalArgumentException if {@code value} is negative or greater
   *     than 255
   */
  public static byte checkedCast(long value) {
    checkArgument(value >> 8 == 0, "out of range: %s", value);
    return (byte) value;
  }

  /**
   * Returns the {@code byte} value that, when treated as unsigned, is nearest
   * in value to {@code value}.
   *
   * @param value any {@code long} value
   * @return {@code (byte) 255} if {@code value >= 255}, {@code (byte) 0} if
   *     {@code value <= 0}, and {@code value} cast to {@code byte} otherwise
   */
  public static byte saturatedCast(long value) {
    if (value > 255) {
      return (byte) 255; // -1
    }
    if (value < 0) {
      return (byte) 0;
    }
    return (byte) value;
  }

  /**
   * Compares the two specified {@code byte} values, treating them as unsigned
   * values between 0 and 255 inclusive. For example, {@code (byte) -127} is
   * considered greater than {@code (byte) 127} because it is seen as having
   * the value of positive {@code 129}.
   *
   * @param a the first {@code byte} to compare
   * @param b the second {@code byte} to compare
   * @return a negative value if {@code a} is less than {@code b}; a positive
   *     value if {@code a} is greater than {@code b}; or zero if they are equal
   */
  public static int compare(byte a, byte b) {
    return toInt(a) - toInt(b);
  }

  /**
   * Returns the least value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of {@code byte} values
   * @return the value present in {@code array} that is less than or equal to
   *     every other value in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static byte min(byte... array) {
    checkArgument(array.length > 0);
    int min = toInt(array[0]);
    for (int i = 1; i < array.length; i++) {
      int next = toInt(array[i]);
      if (next < min) {
        min = next;
      }
    }
    return (byte) min;
  }

  /**
   * Returns the greatest value present in {@code array}.
   *
   * @param array a <i>nonempty</i> array of {@code byte} values
   * @return the value present in {@code array} that is greater than or equal
   *     to every other value in the array
   * @throws IllegalArgumentException if {@code array} is empty
   */
  public static byte max(byte... array) {
    checkArgument(array.length > 0);
    int max = toInt(array[0]);
    for (int i = 1; i < array.length; i++) {
      int next = toInt(array[i]);
      if (next > max) {
        max = next;
      }
    }
    return (byte) max;
  }

  /**
   * Returns a string containing the supplied {@code byte} values separated by
   * {@code separator}. For example, {@code join(":", (byte) 1, (byte) 2,
   * (byte) 255)} returns the string {@code "1:2:255"}.
   *
   * @param separator the text that should appear between consecutive values in
   *     the resulting string (but not at the start or end)
   * @param array an array of {@code byte} values, possibly empty
   */
  public static String join(String separator, byte... array) {
    checkNotNull(separator);
    if (array.length == 0) {
      return "";
    }

    // For pre-sizing a builder, just get the right order of magnitude
    StringBuilder builder = new StringBuilder(array.length * 5);
    builder.append(toInt(array[0]));
    for (int i = 1; i < array.length; i++) {
      builder.append(separator).append(toInt(array[i]));
    }
    return builder.toString();
  }

  /**
   * Returns a comparator that compares two {@code byte} arrays
   * lexicographically. That is, it compares, using {@link
   * #compare(byte, byte)}), the first pair of values that follow any common
   * prefix, or when one array is a prefix of the other, treats the shorter
   * array as the lesser. For example, {@code [] < [0x01] < [0x01, 0x7F] <
   * [0x01, 0x80] < [0x02]}. Values are treated as unsigned.
   *
   * <p>The returned comparator is inconsistent with {@link
   * Object#equals(Object)} (since arrays support only identity equality), but
   * it is consistent with {@link java.util.Arrays#equals(byte[], byte[])}.
   *
   * @see <a href="http://en.wikipedia.org/wiki/Lexicographical_order">
   *     Lexicographical order article at Wikipedia</a>
   * @since 2.0
   */
  public static Comparator<byte[]> lexicographicalComparator() {
    return LexicographicalComparatorHolder.BEST_COMPARATOR;
  }

  @VisibleForTesting
  static Comparator<byte[]> lexicographicalComparatorJavaImpl() {
    return LexicographicalComparatorHolder.PureJavaComparator.INSTANCE;
  }

  /**
   * Provides a lexicographical comparator implementation; either a Java
   * implementation or a faster implementation based on {@link Unsafe}.
   *
   * <p>Uses reflection to gracefully fall back to the Java implementation if
   * {@code Unsafe} isn't available.
   */
  @VisibleForTesting
  static class LexicographicalComparatorHolder {
    static final String UNSAFE_COMPARATOR_NAME =
        LexicographicalComparatorHolder.class.getName() + "$UnsafeComparator";

    // BEGIN android-changed

    static final Comparator<byte[]> BEST_COMPARATOR = lexicographicalComparatorJavaImpl();

    // @VisibleForTesting
    // enum UnsafeComparator implements Comparator<byte[]> {
    //   INSTANCE;

    //   static final boolean littleEndian =
    //       ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);

    //   /*
    //    * The following static final fields exist for performance reasons.
    //    *
    //    * In UnsignedBytesBenchmark, accessing the following objects via static
    //    * final fields is the fastest (more than twice as fast as the Java
    //    * implementation, vs ~1.5x with non-final static fields, on x86_32)
    //    * under the Hotspot server compiler. The reason is obviously that the
    //    * non-final fields need to be reloaded inside the loop.
    //    *
    //    * And, no, defining (final or not) local variables out of the loop still
    //    * isn't as good because the null check on the theUnsafe object remains
    //    * inside the loop and BYTE_ARRAY_BASE_OFFSET doesn't get
    //    * constant-folded.
    //    *
    //    * The compiler can treat static final fields as compile-time constants
    //    * and can constant-fold them while (final or not) local variables are
    //    * run time values.
    //    */

    //   static final Unsafe theUnsafe;

    //   /** The offset to the first element in a byte array. */
    //   static final int BYTE_ARRAY_BASE_OFFSET;

    //   static {
    //     theUnsafe = (Unsafe) AccessController.doPrivileged(
    //         new PrivilegedAction<Object>() {
    //           @Override
    //           public Object run() {
    //             try {
    //               Field f = Unsafe.class.getDeclaredField("theUnsafe");
    //               f.setAccessible(true);
    //               return f.get(null);
    //             } catch (NoSuchFieldException e) {
    //               // It doesn't matter what we throw;
    //               // it's swallowed in getBestComparator().
    //               throw new Error();
    //             } catch (IllegalAccessException e) {
    //               throw new Error();
    //             }
    //           }
    //         });

    //     BYTE_ARRAY_BASE_OFFSET = theUnsafe.arrayBaseOffset(byte[].class);

    //     // sanity check - this should never fail
    //     if (theUnsafe.arrayIndexScale(byte[].class) != 1) {
    //       throw new AssertionError();
    //     }
    //   }

    //   @Override public int compare(byte[] left, byte[] right) {
    //     int minLength = Math.min(left.length, right.length);
    //     int minWords = minLength / Longs.BYTES;

    //     /*
    //      * Compare 8 bytes at a time. Benchmarking shows comparing 8 bytes at a
    //      * time is no slower than comparing 4 bytes at a time even on 32-bit.
    //      * On the other hand, it is substantially faster on 64-bit.
    //      */
    //     for (int i = 0; i < minWords * Longs.BYTES; i += Longs.BYTES) {
    //       long lw = theUnsafe.getLong(left, BYTE_ARRAY_BASE_OFFSET + (long) i);
    //       long rw = theUnsafe.getLong(right, BYTE_ARRAY_BASE_OFFSET + (long) i);
    //       long diff = lw ^ rw;

    //       if (diff != 0) {
    //         if (!littleEndian) {
    //           return UnsignedLongs.compare(lw, rw);
    //         }

    //         // Use binary search
    //         int n = 0;
    //         int y;
    //         int x = (int) diff;
    //         if (x == 0) {
    //           x = (int) (diff >>> 32);
    //           n = 32;
    //         }

    //         y = x << 16;
    //         if (y == 0) {
    //           n += 16;
    //         } else {
    //           x = y;
    //         }

    //         y = x << 8;
    //         if (y == 0) {
    //           n += 8;
    //         }
    //         return (int) (((lw >>> n) & 0xFFL) - ((rw >>> n) & 0xFFL));
    //       }
    //     }

    //     // The epilogue to cover the last (minLength % 8) elements.
    //     for (int i = minWords * Longs.BYTES; i < minLength; i++) {
    //       int result = UnsignedBytes.compare(left[i], right[i]);
    //       if (result != 0) {
    //         return result;
    //       }
    //     }
    //     return left.length - right.length;
    //   }
    // }

    // END android-changed

    enum PureJavaComparator implements Comparator<byte[]> {
      INSTANCE;

      @Override public int compare(byte[] left, byte[] right) {
        int minLength = Math.min(left.length, right.length);
        for (int i = 0; i < minLength; i++) {
          int result = UnsignedBytes.compare(left[i], right[i]);
          if (result != 0) {
            return result;
          }
        }
        return left.length - right.length;
      }
    }

    // BEGIN android-changed

    // /**
    //  * Returns the Unsafe-using Comparator, or falls back to the pure-Java
    //  * implementation if unable to do so.
    //  */
    // static Comparator<byte[]> getBestComparator() {
    //   try {
    //     Class<?> theClass = Class.forName(UNSAFE_COMPARATOR_NAME);

    //     // yes, UnsafeComparator does implement Comparator<byte[]>
    //     @SuppressWarnings("unchecked")
    //     Comparator<byte[]> comparator =
    //         (Comparator<byte[]>) theClass.getEnumConstants()[0];
    //     return comparator;
    //   } catch (Throwable t) { // ensure we really catch *everything*
    //     return lexicographicalComparatorJavaImpl();
    //   }
    // }

    // END android-changed

  }
}

