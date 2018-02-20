package ru.ifmo.rain.khusainov.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {
    final private List<E> sortedList;
    final private Comparator<? super E> comparator;

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(ArraySet<E> other) {
        this(other.sortedList, other.comparator);
    }

    private ArraySet(List<E> list, Comparator<? super E> comparator) {
        sortedList = list;
        this.comparator = comparator;
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<? extends E> other, Comparator<? super E> comparator) {
        Set<E> buffer  = new TreeSet<>(comparator);
        buffer.addAll(other);
        this.comparator = comparator;
        sortedList = new ArrayList<>(buffer);
    }

    private int getBinarySearchIndex(int existShift, int absentShift, E elem) {
        int index = Collections.binarySearch(sortedList, elem, comparator);
        if (index >= 0) {
            return index + existShift;
        } else {
            return -index - 1 + absentShift;
        }
    }

    private boolean isCorrectIndex(int index) {
        return index >= 0 && index < sortedList.size();
    }

    private E safeGetter(int index) {
        return isCorrectIndex(index) ? sortedList.get(index) : null;
    }

    private E safeGetter(int existShift, int absentShift, E e) {
        return safeGetter(getBinarySearchIndex(existShift, absentShift, e));
    }

    @Override
    public E lower(E e) {
        return safeGetter(-1, -1, e);
    }

    @Override
    public E floor(E e) {
        return safeGetter(0, -1, e);
    }

    @Override
    public E ceiling(E e) {
        return safeGetter(0, 0, e);
    }

    @Override
    public E higher(E e) {
        return safeGetter(1, 0, e);
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }

    @Override
    public int size() {
        return sortedList == null ? 0 : sortedList.size();
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.unmodifiableList(sortedList).iterator();
    }

    private class DescendingList<T> extends AbstractList<T> {
        private final List<T> sortedList;
        private boolean isReversed;

        DescendingList(List<T> list) {
            if (list instanceof DescendingList) {
                sortedList = ((DescendingList<T>) list).sortedList;
                isReversed = !((DescendingList<T>) list).isReversed;
            } else {
                sortedList = list;
                isReversed = true;
            }
        }

        @Override
        public T get(int index) {
            return isReversed ? sortedList.get(size() - 1 - index) : sortedList.get(index);
        }

        @Override
        public int size() {
            return sortedList.size();
        }
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(new DescendingList<>(sortedList), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingList<>(sortedList).iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int left = getBinarySearchIndex(fromInclusive ? 0 : 1, 0, fromElement);
        int right = getBinarySearchIndex(toInclusive ? 0 : -1, -1, toElement) + 1;
        if (left > right) {
            left = right;
        }
        return new ArraySet<>(sortedList.subList(left, right), comparator);
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return isEmpty() ? Collections.emptyNavigableSet() : subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return isEmpty() ? Collections.emptyNavigableSet() : subSet(fromElement, inclusive, last(), true);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return sortedList.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return sortedList.get(size() - 1);
    }

    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(sortedList, (E) o, comparator) >= 0;
    }
}
