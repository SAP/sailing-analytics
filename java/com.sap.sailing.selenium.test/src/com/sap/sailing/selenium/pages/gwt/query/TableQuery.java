package com.sap.sailing.selenium.pages.gwt.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.query.operation.bool.And;

public class TableQuery<S extends DataEntryPO> {
    private CellTablePO<? extends S> table;
    
    private Predicate where;
    
    public TableQuery() {
    }
    
    public <Q extends CellTablePO<? extends S>> TableQuery<S> from(Q table) {
        this.table = table;
        
        return this;
    }
    
    public TableQuery<S> where(Predicate predicate) {
        if (predicate == null)
            return this;
        
        this.where = and(this.where, predicate);
        
        return this;
    }
    
    public TableQuery<S> where(Predicate... predicates) {
        for(Predicate predicate : predicates) {
            where(predicate);
        }
        
        return this;
    }
    
    private Predicate and(Predicate lhs, Predicate rhs) {
        if (lhs == null)
            return rhs;
        
        return new And(lhs, rhs);
    }
    
    /**
     * <p>Returns a single result if there's exactly one result, else throws exception.</p>
     *
     * @return
     *   the first result or null if no result is found.
     */
    public S singleResult() {
        List<S> results = allResults();
        if (results.size() == 1) {
            return results.get(0);
        } else if (results.size() == 0) {
            throw new QueryException("No result found.");
        } else {
            throw new QueryException("Multiple results found.");
        }
    }
    
    /**
     * <p>Returns a single result or null if no result is found. For multiple results only the first one is returned.</p>
     *
     * @return
     *   the first result or null if no result is found.
     */
    public S firstResult() {
        List<S> results = allResults();
        
        return (results.isEmpty() ? null : results.get(0));
    }
    
    /**
     * <p>Returns a unique result or null if no result is found.</p>
     *
     * @throws QueryException
     *   if there is more than one matching result.
     * @return
     *   a unique result or null if no result is found.
     */
    public S uniqueResult() {
        List<S> results = allResults();
        
        if(results.size() > 1)
            throw new QueryException("There is more than one matching result.");
        
        return (results.isEmpty() ? null : results.get(0));
    }
    
    public List<S> allResults() {
        if(this.table != null) {
            return evaluate(this.table.getEntries());
        }
        
        return Collections.emptyList();
    }
    
    private List<S> evaluate(Iterable<? extends S> iterable) {
        List<S> result = new ArrayList<>();
        
        for(S object : iterable) {
            Boolean matches = this.where.evaluate(object);
            
            if(matches.booleanValue())
                result.add(object);
        }
        
        return result;
    }
}
