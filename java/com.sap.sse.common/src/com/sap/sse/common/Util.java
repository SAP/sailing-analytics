package com.sap.sse.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sse.common.util.NaturalComparator;


public class Util {

    public static class Pair<A, B> implements Serializable {
        private static final long serialVersionUID = -7631774746419135931L;
    
        private A a;
    
        private B b;
    
        private transient int hashCode;
    
        @SuppressWarnings("unused")
        // required for some serialization frameworks such as GWT RPC
        private Pair() {
        }

        public Pair(A a, B b) {
            this.a = a;
            this.b = b;
            hashCode = 0;
        }

        public A getA() {
            return a;
        }

        public B getB() {
            return b;
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = 17;
                hashCode = 37 * hashCode + (a != null ? a.hashCode() : 0);
                hashCode = 37 * hashCode + (b != null ? b.hashCode() : 0);
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            boolean result;
            if (this == obj) {
                result = true;
            } else if (obj instanceof Pair<?, ?>) {
                Pair<?, ?> pair = (Pair<?, ?>) obj;
                result = (this.a != null && this.a.equals(pair.a) || this.a == null && pair.a == null)
                        && (this.b != null && this.b.equals(pair.b) || this.b == null && pair.b == null);
            } else {
                result = false;
            }
            return result;
        }

        @Override
        public String toString() {
            return "[" + (a == null ? "null" : a.toString()) + ", " + (b == null ? "null" : b.toString()) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    public static class Triple<A, B, C> implements Serializable {
        private static final long serialVersionUID = 6806146864367514601L;

        private A a;

        private B b;

        private C c;

        private transient int hashCode;

        @SuppressWarnings("unused")
        // required for some serialization frameworks such as GWT RPC
        private Triple() {
        }

        public Triple(A a, B b, C c) {
            this.a = a;
            this.b = b;
            this.c = c;
            hashCode = 0;
        }

        public A getA() {
            return a;
        }

        public B getB() {
            return b;
        }

        public C getC() {
            return c;
        }

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = 17;
                hashCode = 37 * hashCode + (a != null ? a.hashCode() : 0);
                hashCode = 37 * hashCode + (b != null ? b.hashCode() : 0);
                hashCode = 37 * hashCode + (c != null ? c.hashCode() : 0);
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object obj) {
            boolean result;
            if (this == obj) {
                result = true;
            } else if (obj instanceof Triple<?, ?, ?>) {
                Triple<?, ?, ?> thrice = (Triple<?, ?, ?>) obj;
                result = (this.a != null && this.a.equals(thrice.a) || this.a == null && thrice.a == null)
                        && (this.b != null && this.b.equals(thrice.b) || this.b == null && thrice.b == null)
                        && (this.c != null && this.c.equals(thrice.c) || this.c == null && thrice.c == null);
            } else {
                result = false;
            }
            return result;
        }

        @Override
        public String toString() {
            return "[" + a + ", " + b + ", " + c + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
    }
    
    /**
     * To be replaced with java.util.function.Supplier when we can consistently use Java 8.
     */
    public interface Provider<T> {
        T get();
    }
    
    /**
     * To be replaced with java.util.function.Function when we can consistently use Java 8.
     */
    public interface Function<I, O> {
        O get(I in);
    }

    /**
     * Adds all elements from <code>what</code> to <code>addTo</code> and returns <code>addTo</code> for chained use.
     * If <code>what</code> is <code>null</code>, this operation does nothing, not even fail with an exception, but
     * return the unmodified <code>addTo</code>.
     */
    public static <T> Collection<T> addAll(Iterable<? extends T> what, Collection<T> addTo) {
        if (what != null) {
            for (T t : what) {
                addTo.add(t);
            }
        }
        return addTo;
    }
    
    /**
     * Adds <code>v</code> to the value set for key <code>k</code>. If no entry exists yet for <code>k</code>, the
     * entry is created using a {@link HashSet} for the value set.
     */
    public static <K, V> void add(Map<K, Set<V>> map, K k, V v) {
        Set<V> set = map.get(k);
        if (set == null) {
            set = new HashSet<>();
            map.put(k, set);
        }
        set.add(v);
    }

    /**
     * Removes all elements in <code>what</code> from <code>removeFrom</code> and returns <code>removeFrom</code> for chained use.
     * If <code>what</code> is <code>null</code>, this operation does nothing, not even fail with an exception, but
     * return the unmodified <code>removeFrom</code>.
     */
    public static <T> Collection<T> removeAll(Iterable<T> what, Collection<T> removeFrom) {
        if (what != null) {
            for (T t : what) {
                removeFrom.remove(t);
            }
        }
        return removeFrom;
    }

    public static <T> T[] toArray(Iterable<? extends T> what, T[] arr) {
        List<T> list = new ArrayList<T>();
        addAll(what, list);
        return list.toArray(arr);
    }

    public static <T> int size(Iterable<T> i) {
        if (i instanceof Collection<?>) {
            return ((Collection<?>) i).size();
        } else {
            int result = 0;
            Iterator<T> iter = i.iterator();
            while (iter.hasNext()) {
                result++;
                iter.next();
            }
            return result;
        }
    }

    public static <T> int indexOf(Iterable<? extends T> i, T t) {
        int result;
        if (i instanceof List<?>) {
            List<?> list = (List<?>) i;
            result = list.indexOf(t);
        } else {
            boolean found = false;
            int counter = 0;
            for (T it : i) {
                if (it == null && t == null
                        || it != null && it.equals(t)) {
                    result = counter;
                    found = true;
                    break;
                }
                counter++;
            }
            if (found) {
                result = counter;
            } else {
                result = -1;
            }
        }
        return result;
    }

    public static <T> boolean equals(Iterable<? extends T> a, Iterable<? extends T> b) {
        if (a == null) {
            return b == null;
        } else if (b == null) {
            return a == null;
        } else {
            // neither a nor b are null at this point:
            Iterator<? extends T> aIter = a.iterator();
            Iterator<? extends T> bIter = b.iterator();
            while (aIter.hasNext() && bIter.hasNext()) {
                T ao = aIter.next();
                T bo = bIter.next();
                if (!equalsWithNull(ao, bo)) {
                    return false;
                }
            }
            return !aIter.hasNext() && !bIter.hasNext();
        }
    }

    public static <T> T get(Iterable<T> iterable, int i) {
        if (iterable instanceof List<?>) {
            List<T> l = (List<T>) iterable;
            return l.get(i);
        } else {
            final Iterator<T> iter = iterable.iterator();
            T result = iter.next();
            for (int j=0; j<i; j++) {
                result = iter.next();
            }
            return result;
        }
    }
    
    public static <T> T first(Iterable<T> iterable) {
        final Iterator<T> iter = iterable.iterator();
        final T result;
        if (iter.hasNext()) {
            result = iter.next();
        } else {
            result = null;
        }
        return result;
    }
    
    public static <T> T last(Iterable<T> iterable) {
        final T result;
        if (isEmpty(iterable)) {
            result = null;
        } else {
            result = get(iterable, size(iterable)-1);
        }
        return result;
    }
    
    public static <T> List<T> createList(Iterable<T> iterable) {
        List<T> list = new ArrayList<>();
        Iterator<T> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            list.add(iterator.next());
        }
        return list;
    }
    
    public static <T> Set<T> createSet(Iterable<T> iterable) {
        Set<T> set = new HashSet<>();
        Iterator<T> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            set.add(iterator.next());
        }
        return set;
    }

    /**
     * A null-safe check whether <code>t</code> is contained in <code>ts</code>. For <code>ts==null</code> the method
     * immediately returns <code>false</code>.
     */
    public static <T> boolean contains(Iterable<T> ts, Object t) {
        if (ts == null) {
            return false;
        }
        if (ts instanceof Collection<?>) {
            return ((Collection<?>) ts).contains(t);
        } else {
            for (T t2 : ts) {
                if (equalsWithNull(t2, t)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static <T> boolean isEmpty(Iterable<T> ts) {
        if (ts instanceof Collection<?>) {
            return ((Collection<?>) ts).isEmpty();
        } else {
            return !ts.iterator().hasNext();
        }
    }

    public static boolean equalsWithNull(Object o1, Object o2) {
        final boolean result;
        if (o1 == null) {
            result = (o2 == null);
        } else {
            if (o2 == null) {
                result = false;
            } else {
                result = o1.equals(o2);
            }
        }
        return result;
    }
    
    public static boolean equalsWithNull(String s1, String s2, boolean ignoreCase) {
        final String s1LC = ignoreCase?s1==null?null:s1.toLowerCase():s1;
        final String s2LC = ignoreCase?s2==null?null:s2.toLowerCase():s2;
        return equalsWithNull(s1LC, s2LC);
    }

    /**
     * <code>null</code> is permissible for both, <code>o1</code> and <code>o2</code>, where a <code>null</code> value
     * is considered less than a non-null value if <code>nullIsLess</code> is <code>true</code>, greater otherwise.
     */
    public static <T> int compareToWithNull(Comparable<T> o1, T o2, boolean nullIsLess) {
        final int result;
        if (o1 == null) {
            if (o2 == null) {
                result = 0;
            } else {
                result = nullIsLess ? -1 : 1;
            }
        } else {
            if (o2 == null) {
                result = nullIsLess ? 1 : -1;
            } else {
                result = o1.compareTo(o2);
            }
        }
        return result;
    }

    /**
     * Return the default value instead of null, if the map does not contain the key.
     */
    public static <K, V> V get(Map<K, V> map, K key, V defaultVal) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return defaultVal;
    }

    /**
     * Ensures that a {@link Set Set&lt;V&gt;} is contained in {@code map} for {@code key} and
     * then adds {@code value} to that set. No synchronization / concurrency control effort is
     * made. This is the caller's obligation.
     */
    public static <K, V> void addToValueSet(Map<K, Set<V>> map, K key, V value) {
        addToValueSet(map, key, value, new ValueSetConstructor<V>() {
            @Override
            public Set<V> createSet() {
                return new HashSet<V>();
            }
        });
    }

    public static interface ValueSetConstructor<T> {
        Set<T> createSet();
    }
    
    /**
     * Ensures that a {@link Set Set&lt;V&gt;} is contained in {@code map} for {@code key} and
     * then adds {@code value} to that set. No synchronization / concurrency control effort is
     * made. This is the caller's obligation.
     */
    public static <K, V> void addToValueSet(Map<K, Set<V>> map, K key, V value, ValueSetConstructor<V> setConstructor) {
        Set<V> set = map.get(key);
        if (set == null) {
            set = setConstructor.createSet();
            map.put(key, set);
        }
        set.add(value);
    }

    /**
     * Removes {@code value} from all sets contained as values in {@code map}. If a set is emptied by this removal it is
     * removed from the map. No synchronization / concurrency control effort is made. This is the caller's obligation.
     */
    public static <K, V> void removeFromAllValueSets(Map<K, Set<V>> map, V value) {
        for (final Iterator<Entry<K, Set<V>>> i=map.entrySet().iterator(); i.hasNext(); ) {
            final Entry<K, Set<V>> e = i.next();
            e.getValue().remove(value);
            if (e.getValue().isEmpty()) {
                i.remove();
            }
        }
    }
    
    /**
     * Removes {@code value} from the set that is the value for {@code key} in {@code map} if that key exists. If the
     * set existed and is emptied by this removal it is removed from the map. No synchronization / concurrency control
     * effort is made. This is the caller's obligation.
     */
    public static <K, V> void removeFromValueSet(Map<K, Set<V>> map, K key, V value) {
        final Set<V> valuesPerKey = map.get(key);
        if (valuesPerKey != null) {
            if (valuesPerKey.remove(value) && valuesPerKey.isEmpty()) {
                map.remove(key);
            }
        }
    }

    public static String join(String separator, String... strings) {
        return joinStrings(separator, Arrays.asList(strings));
    }

    public static String joinStrings(String separator, Iterable<String> strings) {
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String string : strings) {
            if (first) {
                first = false;
            } else {
                result.append(separator);
            }
            result.append(string);
        }
        return result.toString();
    }

    public static String join(String separator, Iterable<? extends Named> nameds) {
        return join(separator, toArray(nameds, new Named[0]));
    }

    public static String join(String separator, Named... nameds) {
        String[] strings = new String[nameds.length];
        for (int i=0; i<nameds.length; i++) {
            strings[i] = nameds[i].getName();
        }
        return join(separator, strings);
    }
    
    /**
     * Splits {@code s} along whitespace (blank, tab, line feed, carriage return, form feed) characters that are not
     * within a <em>phrase</em>. <em>Phrases</em> are enclosed by double quotes ({@code "}). To make a double quote or any other
     * character part of a {@link String} in the result, a backslash ({@code \}) must precede the double quote as an escape character. With this,
     * a {@code \} character or a whitespace character can become part of the split result by escaping it with a {@code \} character. If {@code s}'s last character
     * happens to be the (unescaped) escape character it stands for itself.
     * 
     * A double quote {@code "} in the middle of an unquoted phrase marks the beginning of a new quoted phrase. When occurring unescaped in
     * a quoted phrase, it marks the end of that quoted phrase, and a new unquoted phrase starts.
     * <p>
     * 
     * The following example expressions all evaluate to {@code true}:
     * 
     * <pre>
     * {@link #splitAlongWhitespaceRespectingDoubleQuotedPhrases(String) splitAlongWhitespaceRespectingDoubleQuotedPhrases("a b c")}.equals(Arrays.asList("a", "b", "c"))
     * {@link #splitAlongWhitespaceRespectingDoubleQuotedPhrases(String) splitAlongWhitespaceRespectingDoubleQuotedPhrases("a \"b c\"")}.equals(Arrays.asList("a", "b c"))
     * {@link #splitAlongWhitespaceRespectingDoubleQuotedPhrases(String) splitAlongWhitespaceRespectingDoubleQuotedPhrases("a \"b \\\" c\"")}.equals(Arrays.asList("a", "b \" c"))
     * {@link #splitAlongWhitespaceRespectingDoubleQuotedPhrases(String) splitAlongWhitespaceRespectingDoubleQuotedPhrases("a \"bc\"de")}.equals(Arrays.asList("a", "bc", "de"))
     * {@link #splitAlongWhitespaceRespectingDoubleQuotedPhrases(String) splitAlongWhitespaceRespectingDoubleQuotedPhrases("a\"bc\" de")}.equals(Arrays.asList("a", "bc", "de"))
     * {@link #splitAlongWhitespaceRespectingDoubleQuotedPhrases(String) splitAlongWhitespaceRespectingDoubleQuotedPhrases("\\ ")}.equals(Arrays.asList(" "))
     * {@link #splitAlongWhitespaceRespectingDoubleQuotedPhrases(String) splitAlongWhitespaceRespectingDoubleQuotedPhrases("  \\ \\\\ ")}.equals(Arrays.asList(" \\"))
     * {@link #isEmpty(Iterable) isEmpty(splitAlongWhitespaceRespectingDoubleQuotedPhrases(" \n\t  "))
     * </pre>
     * 
     * @return if {@code s==null}, then {@code null}, else a non-{@code null} but possibly empty sequence of {@link Strings} whose iteration order corresponds with
     *         the occurrence of the split results, left to right, in {@code s}
     */
    public static Iterable<String> splitAlongWhitespaceRespectingDoubleQuotedPhrases(String s) {
        final char ESCAPE_CHARACTER = '\\';
        final List<String> result;
        if (s == null) {
            result = null;
        } else {
            result = new ArrayList<>();
            boolean escaped = false;
            StringBuilder phrase = null;
            boolean inQuotedPhrase = false;
            for (final char c : s.toCharArray()) {
                if (escaped) {
                    if (phrase == null) {
                        phrase = new StringBuilder();
                    }
                    phrase.append(c);
                    escaped = false;
                } else if (c == ESCAPE_CHARACTER) {
                    escaped = true; // don't append but mark for next character to be appended
                } else if (c == '"') {
                    if (inQuotedPhrase) {
                        result.add(phrase.toString());
                        inQuotedPhrase = false;
                        phrase = null;
                    } else {
                        inQuotedPhrase = true;
                        if (phrase != null) { // starts a quoted phrase in the middle of a running phrase
                            result.add(phrase.toString());
                        }
                        phrase = new StringBuilder();
                    }
                } else {
                    if (inQuotedPhrase) {
                        phrase.append(c);
                    } else {
                        if (new String(new char[] {c}).matches("\\s")) { // whitespace
                            if (phrase != null) {
                                // phrase is terminated by this whitespace
                                result.add(phrase.toString());
                                phrase = null;
                            } // else skip whitespace outside of phrases
                        } else {
                            if (phrase == null) {
                                phrase = new StringBuilder();
                            }
                            phrase.append(c);
                        }
                    }
                }
            }
            if (escaped) {
                // escape character as last character stands for itself
                if (phrase == null) {
                    phrase = new StringBuilder();
                }
                phrase.append(ESCAPE_CHARACTER);
            }
            if (phrase != null) {
                // a phrase is also terminated by the end of the string, also (lenient mode) if
                // within an (unterminated) quoted phrase
                result.add(phrase.toString());
            }
        }
        return result;
    }
    
    /**
     * Returns the first non-<code>null</code> object in <code>objects</code> or <code>null</code>
     * if no such object exists.
     */
    @SafeVarargs
    public static <T> T getFirstNonNull(T... objects) {
        for (T t : objects) {
            if (t != null) {
                return t;
            }
        }
        return null;
    }
    
    /**
     * Returns the earlier {@link TimePoint} of a and b. If one of them is <code>null</code> and the other
     * <code>!null</code>, the TimePoint that is not <code>!null</code> gets returned. If both are <code>null</code>,
     * <code>null</code> is the result.
     */
    public static TimePoint getEarliestOfTimePoints(TimePoint a, TimePoint b) {
        TimePoint result = null;
        if (a != null && b != null) {
            result = a.before(b) ? a : b;
        } else {
            result = (a != null && b == null) ? a : (a == null && b != null) ? b : null;
        }
        return result;
    }
    
    /**
     * Returns the latest {@link TimePoint} of a and b. If one of them is <code>null</code> and the other
     * <code>!null</code>, the TimePoint that is not <code>!null</code> gets returned. If both are <code>null</code>,
     * <code>null</code> is the result.
     */
    public static TimePoint getLatestOfTimePoints(TimePoint a, TimePoint b) {
        TimePoint result = null;
        if (a != null && b != null) {
            result = a.after(b) ? a : b;
        } else {
            result = (a != null && b == null) ? a : (a == null && b != null) ? b : null;
        }
        return result;
    }
    
    /**
     * Returns <code>true</code> if <code>timePoint</code> is after <code>a</code> an before <code>b</code>.
     * If one of the parameters is <code>null</code> the method returns <code>false</code>.
     */
    public static boolean isTimePointInRangeOfTimePointsAandB(TimePoint timePoint, TimePoint a, TimePoint b) {
        boolean result = false;
        if (timePoint != null && a != null && b != null) {
            result = timePoint.after(a) && timePoint.before(b);
        }
        return result;
    }
    
    /**
     * Searches the dominant object in an <code>Iterable&lt;T&gt;</code> collection.
     * 
     * @param objects
     *            The <code>Iterable&lt;T&gt;</code> collection which should be analyzed. Objects are compared
     *            by their definition of {@link Object#equals(Object)}.
     * @return <code>T</code> Returns the dominant object. If the collection have two objects with the highest count,
     *         you will get one of them returned. If the collection is <code>null</code> or empty, the method will
     *         return <code>null</code>.
     */
    public static <T> T getDominantObject(Iterable<T> objects) {
        T result = null;
        if (objects != null) {
            if (objects.iterator().hasNext()) {
                HashMap<T, Integer> countPerObject = new HashMap<>();
                int highestCount = 0;
                for (T it : objects) {
                    Integer objectCount = countPerObject.get(it);
                    if (objectCount == null) {
                        objectCount = 0;
                    }
                    objectCount++;
                    countPerObject.put(it, objectCount);
                    if (objectCount > highestCount) {
                        highestCount = objectCount;
                        result = it;
                    }
                }
            }
        }
        return result;
    }

    public static <T> List<T> asList(Iterable<T> visibleCourseAreas) {
        ArrayList<T> list = new ArrayList<T>();
        addAll(visibleCourseAreas, list);
        return list;
    }

    public static <T> List<T> cloneListOrNull(List<T> list) {
        final List<T> result;
        if (list == null) {
            result = null;
        } else {
            result = new ArrayList<T>(list);
        }
        return result;
    }

    public static <T extends Named> List<T> sortNamedCollection(Collection<T> collection) {
        List<T> sortedCollection = new ArrayList<>(collection);
        Collections.sort(sortedCollection, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return new NaturalComparator().compare(o1.getName(), o2.getName());
            }
        });
        return sortedCollection;
    }
    
    /**
     * Groups the given values by a key. The key is being extracted from the values by using the given {@link Function}. Inner
     * Collections of the resulting Map are created using the given {@link Provider} instance.
     * <br>
     * Can be replaced with Java 8 Stream API in the future.
     * 
     * @param values the values to group
     * @param mappingFunction function that extracts the group key from a value
     * @param newCollectionProvider factory to create new instances of the inner collections
     * @return a map containing all given values in inner collections grouped by a specific criteria
     */
    public static <K, V> Map<K, Iterable<V>> group(Iterable<V> values, Function<V, K> mappingFunction,
            Provider<? extends Collection<V>> newCollectionProvider) {
        final Map<K, Iterable<V>> result = new HashMap<>();
        for (V value : values) {
            final K key = mappingFunction.get(value);
            Collection<V> groupValues = (Collection<V>) result.get(key);
            if (groupValues == null) {
                groupValues = newCollectionProvider.get();
                result.put(key, groupValues);
            }
            groupValues.add(value);
        }
        return result;
    }
    
    @SafeVarargs
    public static <T extends Comparable<T>> T min(T... elements) {
        return Collections.min(Arrays.asList(elements));
    }

    @SafeVarargs
    public static <T extends Comparable<T>> T max(T... elements) {
        return Collections.max(Arrays.asList(elements));
    }
}