package com.sap.sailing.selenium.pages.gwt.query;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * <p>PathMetadata provides metadata for Path expressions.</p>
 * 
 * @author D049941
 *
 * @param <T>
 *   
 */
public class PathMetadata<T> {
    // QUESTION [D049941]: What is the parent good for?
    private final Path<?> parent;

    private final Method method;
    
    private final Object[] arguments;
    
    // QUESTION [D049941]: What is the root good for?
    private final Path<?> root;
    
    public PathMetadata(Path<?> parent, Method method, Object[] arguments) {
        this.parent = parent;
        this.method = method;
        this.arguments = arguments;
        this.root = (parent != null ? parent.getRoot() : null);
    }
    
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        
        if (!(object instanceof PathMetadata<?>)) {
            return false;
        }
        
        PathMetadata<?> other = (PathMetadata<?>) object;
        
        if(!Objects.equals(this.parent, other.parent))
            return false;
        
        if(!this.method.equals(other.method))
            return false;
        
        if(!Arrays.equals(this.arguments, other.arguments))
            return false;
        
        return true;
    }
    
    public boolean isRoot() {
        return this.parent == null;// || (pathType == PathType.DELEGATE && parent.getMetadata().isRoot()); 
    }
    
    public String getName() {
        return this.method.getName();
    }
    
    public Path<?> getParent() {
        return this.parent;
    }

    public Path<?> getRoot() {
        return this.root;
    }
    
    public Method getMethod() {
        return this.method;
    }
    
    public Object[] getArguments() {
        return this.arguments;
    }
    
    
    @Override
    public int hashCode() {
        return 31 * this.method.hashCode() + Arrays.hashCode(this.arguments);
    }
}
