/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.BoundType.CLOSED;

import com.google.common.annotations.GwtCompatible;

import java.util.Collection;

import javax.annotation.Nullable;

/**
 * An implementation of {@link ContiguousSet} that contains one or more elements.
 *
 * @author Gregory Kick
 */
@GwtCompatible(emulated = true)
@SuppressWarnings("unchecked") // allow ungenerified Comparable types
final class RegularContiguousSet<C extends Comparable> extends ContiguousSet<C> {
  private final Range<C> range;

  RegularContiguousSet(Range<C> range, DiscreteDomain<C> domain) {
    super(domain);
    this.range = range;
  }

  // Abstract method doesn't exist in GWT emulation
  /* @Override */ ContiguousSet<C> headSetImpl(C toElement, boolean inclusive) {
    return range.intersection(Ranges.upTo(toElement, BoundType.forBoolean(inclusive)))
        .asSet(domain);
  }

  // Abstract method doesn't exist in GWT emulation
  /* @Override */ int indexOf(Object target) {
    return contains(target) ? (int) domain.distance(first(), (C) target) : -1;
  }

  // Abstract method doesn't exist in GWT emulation
  /* @Override */ ContiguousSet<C> subSetImpl(C fromElement, boolean fromInclusive, C toElement,
      boolean toInclusive) {
    return range.intersection(Ranges.range(fromElement, BoundType.forBoolean(fromInclusive),
        toElement, BoundType.forBoolean(toInclusive))).asSet(domain);
  }

  // Abstract method doesn't exist in GWT emulation
  /* @Override */ ContiguousSet<C> tailSetImpl(C fromElement, boolean inclusive) {
    return range.intersection(Ranges.downTo(fromElement, BoundType.forBoolean(inclusive)))
        .asSet(domain);
  }

  @Override public UnmodifiableIterator<C> iterator() {
    return new AbstractLinkedIterator<C>(first()) {
      final C last = last();

      @Override
      protected C computeNext(C previous) {
        return equalsOrThrow(previous, last) ? null : domain.next(previous);
      }
    };
  }

  private static boolean equalsOrThrow(Comparable<?> left, @Nullable Comparable<?> right) {
    return right != null && Range.compareOrThrow(left, right) == 0;
  }

  @Override boolean isPartialView() {
    return false;
  }

  @Override public C first() {
    return range.lowerBound.leastValueAbove(domain);
  }

  @Override public C last() {
    return range.upperBound.greatestValueBelow(domain);
  }

  @Override public int size() {
    long distance = domain.distance(first(), last());
    return (distance >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) distance + 1;
  }

  @Override public boolean contains(Object object) {
    try {
      return range.contains((C) object);
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override public boolean containsAll(Collection<?> targets) {
    try {
      return range.containsAll((Iterable<? extends C>) targets);
    } catch (ClassCastException e) {
      return false;
    }
  }

  @Override public boolean isEmpty() {
    return false;
  }

  // copied to make sure not to use the GWT-emulated version
  @Override public Object[] toArray() {
    return ObjectArrays.toArrayImpl(this);
  }

  // copied to make sure not to use the GWT-emulated version
  @Override public <T> T[] toArray(T[] other) {
    return ObjectArrays.toArrayImpl(this, other);
  }

  @Override public ContiguousSet<C> intersection(ContiguousSet<C> other) {
    checkNotNull(other);
    checkArgument(this.domain.equals(other.domain));
    if (other.isEmpty()) {
      return other;
    } else {
      C lowerEndpoint = Ordering.natural().max(this.first(), other.first());
      C upperEndpoint = Ordering.natural().min(this.last(), other.last());
      return (lowerEndpoint.compareTo(upperEndpoint) < 0)
          ? Ranges.closed(lowerEndpoint, upperEndpoint).asSet(domain)
          : new EmptyContiguousSet<C>(domain);
    }
  }

  @Override public Range<C> range() {
    return range(CLOSED, CLOSED);
  }

  @Override public Range<C> range(BoundType lowerBoundType, BoundType upperBoundType) {
    return Ranges.create(range.lowerBound.withLowerBoundType(lowerBoundType, domain),
        range.upperBound.withUpperBoundType(upperBoundType, domain));
  }

  @Override public boolean equals(Object object) {
    if (object == this) {
      return true;
    } else if (object instanceof RegularContiguousSet<?>) {
      RegularContiguousSet<?> that = (RegularContiguousSet<?>) object;
      if (this.domain.equals(that.domain)) {
        return this.first().equals(that.first())
            && this.last().equals(that.last());
      }
    }
    return super.equals(object);
  }

  // copied to make sure not to use the GWT-emulated version
  @Override public int hashCode() {
    return Sets.hashCodeImpl(this);
  }

  private static final long serialVersionUID = 0;
}

