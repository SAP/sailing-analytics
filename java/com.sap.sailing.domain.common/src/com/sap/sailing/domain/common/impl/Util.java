package com.sap.sailing.domain.common.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Util {
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
    
    public static <T> T get(Iterable<T> iterable, int i) {
        if (iterable instanceof List<?>) {
            List<T> l = (List<T>) iterable;
            return l.get(i);
        } else {
            Iterator<T> iter = iterable.iterator();
            T result = iter.next();
            for (int j=0; j<i; j++) {
                result = iter.next();
            }
            return result;
        }
    }

    public static <T> boolean contains(Iterable<T> ts, T t) {
        if (ts instanceof Collection<?>) {
            return ((Collection<?>) ts).contains(t);
        } else {
            for (T t2 : ts) {
                if (t2.equals(t)) {
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

    public static class Pair<A, B> implements Serializable {
        private static final long serialVersionUID = -7631774746419135931L;

        private A a;

        private B b;

        private int hashCode;

        @SuppressWarnings("unused") // required for some serialization frameworks such as GWT RPC
        private Pair() {}
        
        public Pair( A a, B b ) {

            this.a = a;
            this.b = b;
            hashCode = 0;
        }

        public void setA( A a ) {

            this.a = a;
            hashCode = 0;
        }

        public A getA( ) {

            return a;
        }

        public void setB( B b ) {

            this.b = b;
            hashCode = 0;
        }

        public B getB( ) {

            return b;
        }

        @Override
        public int hashCode( ) {

            if ( hashCode == 0 ) {
                hashCode = 17;
                hashCode = 37 * hashCode + ( a != null ? a.hashCode( ) : 0 );
                hashCode = 37 * hashCode + ( b != null ? b.hashCode( ) : 0 );
            }
            return hashCode;
        }

        @Override
        public boolean equals( Object obj ) {

            boolean result;
            if ( this == obj ) {
                result = true;
            } else if ( obj instanceof Pair<?, ?> ) {
                Pair<?, ?> pair = (Pair<?, ?>) obj;
                result = ( this.a != null && this.a.equals( pair.a ) || this.a == null && pair.a == null ) && ( this.b != null && this.b.equals( pair.b ) || this.b == null && pair.b == null );
            } else {
                result = false;
            }
            return result;
        }

        @Override
        public String toString( ) {

            return "[" + (a==null?"null":a.toString( )) + ", " +
                (b==null?"null":b.toString( )) + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }

    public static class Triple<A, B, C> implements Serializable {
        private static final long serialVersionUID = 6806146864367514601L;

        private A a;

        private B b;

        private C c;

        private int hashCode;

        @SuppressWarnings("unused") // required for some serialization frameworks such as GWT RPC
        private Triple() {}

        public Triple( A a, B b, C c ) {

            this.a = a;
            this.b = b;
            this.c = c;
            hashCode = 0;
        }

        public void setA( A a ) {

            this.a = a;
            hashCode = 0;
        }

        public A getA( ) {

            return a;
        }

        public void setB( B b ) {

            this.b = b;
            hashCode = 0;
        }

        public B getB( ) {

            return b;
        }

        public void setC( C c ) {

            this.c = c;
            hashCode = 0;
        }

        public C getC( ) {

            return c;
        }

        @Override
        public int hashCode( ) {

            if ( hashCode == 0 ) {
                hashCode = 17;
                hashCode = 37 * hashCode + ( a != null ? a.hashCode( ) : 0 );
                hashCode = 37 * hashCode + ( b != null ? b.hashCode( ) : 0 );
                hashCode = 37 * hashCode + ( c != null ? c.hashCode( ) : 0 );
            }
            return hashCode;
        }

        @Override
        public boolean equals( Object obj ) {

            boolean result;
            if ( this == obj ) {
                result = true;
            } else if ( obj instanceof Triple<?, ?, ?> ) {
                Triple<?, ?, ?> thrice = (Triple<?, ?, ?>) obj;
                result = ( this.a != null && this.a.equals( thrice.a ) || this.a == null && thrice.a == null ) && ( this.b != null && this.b.equals( thrice.b ) || this.b == null && thrice.b == null ) && ( this.c != null && this.c.equals( thrice.c ) || this.c == null && thrice.c == null );
            } else {
                result = false;
            }
            return result;
        }

        @Override
        public String toString( ) {

            return "[" + a + ", " + b + ", " + c + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        }
    }
}
