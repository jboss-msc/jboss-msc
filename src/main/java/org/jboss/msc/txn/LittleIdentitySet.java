/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc.txn;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class LittleIdentitySet<T> extends AbstractSet<T> {

    private static final Object[][] NADA = new Object[0][];
    private static final int MAX_CAPACITY = 1 << 31;

    private int size;

    @SuppressWarnings("unchecked")
    private T[][] table = (T[][]) NADA;

    LittleIdentitySet() {
    }

    LittleIdentitySet(int capacity) {
        if (capacity < 2) {
            capacity = 2;
        } else if (capacity > MAX_CAPACITY) {
            capacity = MAX_CAPACITY;
        }
        table = Arrays.copyOf(table, Integer.highestOneBit(capacity - 1) << 1);
    }

    private static int hash(Object o) {
        int hash = System.identityHashCode(o);
        return (hash << 1) - (hash << 8);
    }

    public boolean contains(final Object o) {
        if (size == 0) {
            return false;
        }
        final T[][] table = this.table;
        final int length = table.length;
        if (length == 0) {
            return false;
        }
        int hc = hash(o);
        T[] row = table[hc & length - 1];
        if (row == null) {
            return false;
        }
        for (final T item : row) {
            if (item == o) {
                return true;
            }
        }
        return false;
    }

    public boolean add(final T t) {
        T[][] table = this.table;
        int length = table.length;
        if (size + 1 > length >> 2 && length < MAX_CAPACITY) {
            // resize
            final int newLength = length << 1;
            final int newMask = newLength - 1;
            final T[][] newTable = Arrays.copyOf(table, newLength);
            for (T[] row : newTable) {
                if (row != null) for (int i = 0, length1 = row.length; i < length1; i++) {
                    final T item = row[i];
                    final int hash = hash(item);
                    if ((hash & length) != 0) {
                        row[i] = null;
                        addItem(newTable, hash & newMask, item);
                    }
                }
            }
            final boolean res = addItem(this.table = newTable, hash(t) & length - 1, t);
            if (res) size ++;
            return res;
        } else {
            final boolean res = addItem(table, hash(t) & length - 1, t);
            if (res) size ++;
            return res;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean addItem(final T[][] table, final int i, final T item) {
        T[] row = table[i];
        if (row == null) {
            row = (T[]) new Object[3];
            row[0] = item;
            return true;
        }
        final int rowLength = row.length;
        for (int j = 0; j < rowLength; j++) {
            final T testItem = row[j];
            if (testItem == null) {
                row[j] = item;
                return true;
            } else if (testItem == item) {
                return false;
            }
        }
        T[] newRow = Arrays.copyOf(row, rowLength + 3);
        newRow[rowLength] = item;
        table[i] = newRow;
        return true;
    }

    public boolean remove(final Object o) {
        final T[][] table = this.table;
        final int length = table.length;
        if (length == 0) {
            return false;
        }
        int hc = hash(o);
        T[] row = table[hc & length - 1];
        if (row == null) {
            return false;
        }
        for (int i = 0, length1 = row.length; i < length1; i++) {
            if (row[i] == o) {
                row[i] = null;
                size--;
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public void clear() {
        table = (T[][]) NADA;
        size = 0;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int i = 0, j = 0;
            T next = null;

            public boolean hasNext() {
                if (next != null) {
                    return true;
                }
                for (;;) {
                    if (i == table.length) {
                        return false;
                    }
                    final T[] row = table[i];
                    if (row == null || j == row.length) {
                        j = 0;
                        i ++;
                    } else {
                        T item = row[j++];
                        if (item != null) {
                            next = item;
                            return true;
                        }
                    }
                }
            }

            public T next() {
                if (! hasNext()) {
                    throw new NoSuchElementException();
                }
                return next;
            }

            public void remove() {
                table[i][j - 1] = null;
                size--;
            }
        };
    }

    public int size() {
        return size;
    }
}
