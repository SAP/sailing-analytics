package com.sap.sse.landscape.aws.common.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.landscape.aws.ApplicationLoadBalancer;
import com.sap.sse.landscape.aws.impl.LoadBalancerRuleInserter;
import com.sap.sse.landscape.aws.impl.LoadBalancerRuleInserter.LoadBalancerAdapter;
import com.sap.sse.landscape.aws.impl.LoadBalancerRuleInserter.RuleAdapter;

public class LoadBalancerRuleInserterTest {
    private static final String RULE_NAME_PREFIX = "Rule ";
    private static final String DEFAULT_RULE_NAME = ApplicationLoadBalancer.DEFAULT_RULE_PRIORITY;
    private LoadBalancerAdapter<TestRuleAdapter> loadBalancerAdapter;
    private LoadBalancerRuleInserter<String, TestRuleAdapter> ruleInserter;

    private static class TestRuleAdapter implements RuleAdapter<TestRuleAdapter> {
        private final boolean isDefault;
        private final String priority;
        private final String ruleArn;

        public TestRuleAdapter(boolean isDefault, int priority, String ruleArn) {
            this(isDefault, "" + priority, ruleArn);
        }
        
        public TestRuleAdapter(boolean isDefault, String priority, String ruleArn) {
            super();
            this.isDefault = isDefault;
            this.priority = priority;
            this.ruleArn = ruleArn;
        }

        @Override
        public boolean isDefault() {
            return isDefault;
        }

        @Override
        public String priority() {
            return priority;
        }

        @Override
        public String ruleArn() {
            return ruleArn;
        }

        @Override
        public TestRuleAdapter copyWithNewPriority(int priorityToUseForRuleCopy) {
            return new TestRuleAdapter(isDefault, priorityToUseForRuleCopy, ruleArn);
        }
    }

    private static class TestLoadBalancerAdapter implements LoadBalancerAdapter<TestRuleAdapter> {
        private static final long serialVersionUID = -4853040303405813921L;
        private Iterable<TestRuleAdapter> rules;

        @Override
        public String getName() {
            return "Test Load Balancer Adapter";
        }

        @Override
        public Iterable<TestRuleAdapter> getRules() {
            return rules;
        }

        @Override
        public void updateLoadBalancerListenerRulePriorities(
                List<Pair<Integer, TestRuleAdapter>> newPrioritiesForExistingRules) {
            rules = Util.map(newPrioritiesForExistingRules, p -> p.getB().copyWithNewPriority(p.getA()));
        }

        @Override
        public void addRules(List<TestRuleAdapter> rulesToAdd) {
            rules = Util.concat(Arrays.asList(rules, rulesToAdd));
        }
    }

    @BeforeEach
    public void setUp() {
        loadBalancerAdapter = new TestLoadBalancerAdapter();
        loadBalancerAdapter.addRules(Arrays.asList(new TestRuleAdapter(/* isDefault */ true, 0, DEFAULT_RULE_NAME)));
        ruleInserter = new LoadBalancerRuleInserter<>(loadBalancerAdapter, ApplicationLoadBalancer.MAX_PRIORITY,
                ApplicationLoadBalancer.MAX_RULES_PER_LOADBALANCER);
        assertUniqueAscendingPriorities();
    }

    @Test
    public void simpleRuleInsertionTest() {
        assertEquals(1, Util.size(getRulesSortedByPriority()));
        addRules(1, true, 1);
        assertEquals(2, Util.size(getRulesSortedByPriority()));
        assertUniqueAscendingPriorities();
    }
    
    @Test
    public void defaultRuleGetSortedByPriority() {
        assertEquals(1, Util.size(getRulesSortedByPriority()));
        addRules(1, true, 1);
        loadBalancerAdapter
                .addRules(Collections.singletonList(new TestRuleAdapter(true, DEFAULT_RULE_NAME, DEFAULT_RULE_NAME)));
        assertEquals(2, Util.size(getRulesSortedByPriority()));
        addRules(1, true, 1);
        assertEquals(3, Util.size(getRulesSortedByPriority()));
    }

    @Test
    public void additionalRuleInsertionTest() {
        assertEquals(1, Util.size(getRulesSortedByPriority()));
        addRules(1, true, 1);
        assertEquals(2, Util.size(getRulesSortedByPriority()));
        addRules(1, true, 2);
        assertUniqueAscendingPriorities();
    }

    @Test
    public void testMassInsert() {
        addRules(1, true, 1, 3, 2);
        assertEquals(4, Util.size(getRulesSortedByPriority()));
        assertUniqueAscendingPriorities();
        assertRuleOrder(1, 3, 2);
    }

    @Test
    public void testInsertBefore() {
        addRules(1, true, 1, 2);
        addRulesBefore(1, true, /* insert before: */ 2, /* insert rule numbers: */ 3, 4);
        assertRuleOrder(1, 3, 4, 2);
    }

    @Test
    public void testContiguousInsertBeforeWithEnoughSpace() {
        addRules(1, true, 1, 2);
        loadBalancerAdapter.addRules(Arrays.asList(new TestRuleAdapter(/* isDefault */ false, 5, RULE_NAME_PREFIX+5)));
        addRulesBefore(1, true, /* insert before: */ 5, /* insert rule numbers: */ 3, 4);
        assertRuleOrder(1, 2, 3, 4, 5);
    }

    @Test
    public void testNonContiguousInsertBeforeWithoutEnoughSpace() {
        addRules(1, true, 1);
        loadBalancerAdapter.addRules(Arrays.asList(new TestRuleAdapter(/* isDefault */ false, 3, RULE_NAME_PREFIX+3)));
        loadBalancerAdapter.addRules(Arrays.asList(new TestRuleAdapter(/* isDefault */ false, 5, RULE_NAME_PREFIX+5)));
        addRulesBefore(1, false, /* insert before: */ 5, /* insert rule numbers: */ 2, 4);
        assertRuleOrder(1, 2, 3, 4, 5);
    }

   @Test
    public void testInsertInHole() {
        addRules(1, true, 1);
        loadBalancerAdapter.addRules(Arrays.asList(new TestRuleAdapter(/* isDefault */ false, 4, RULE_NAME_PREFIX+4)));
        addRules(2, true, 2, 3);
        assertRuleOrder(1, 2, 3, 4);
    }
    
    @Test
    public void testInsertAfterTooSmallAHole() {
        addRules(1, true, 1);
        loadBalancerAdapter.addRules(Arrays.asList(new TestRuleAdapter(/* isDefault */ false, 4, RULE_NAME_PREFIX+4)));
        addRules(2, true, 2, 3, 5);
        assertRuleOrder(1, 4, 2, 3, 5);
    }
    
    @Test
    public void testNonContiguousInsertWithTooSmallAHole() {
        addRules(1, true, 1);
        loadBalancerAdapter.addRules(Arrays.asList(new TestRuleAdapter(/* isDefault */ false, 4, RULE_NAME_PREFIX+4)));
        addRules(2, false, 2, 3, 5);
        assertRuleOrder(1, 2, 3, 4, 5);
    }
    
    @Test
    public void testExceptionForTooManyRules() {
        try {
            addRules(1, true, Util.toArray(IntStream.range(0, 101).boxed()::iterator, new Integer[0]));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testExceptionForTooManyRulesWhenAdding() {
        addRules(1, true, Util.toArray(IntStream.range(0, 50).boxed()::iterator, new Integer[0]));
        try {
            addRules(1, true, Util.toArray(IntStream.range(50, 101).boxed()::iterator, new Integer[0]));
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    @Test
    public void testExceptionForRuleToInsertBeforeNotFound() {
        addRules(1, true, 1);
        try {
            addRulesBefore(1, true, Optional.of(new TestRuleAdapter(/* isDefault */ false, 2, RULE_NAME_PREFIX+2)), 2);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
    
    private void addRules(int priority, boolean forceContiguous, Integer... ruleNumbers) {
        addRulesBefore(priority, forceContiguous, /* insertBefore */ Optional.empty(), ruleNumbers);
    }

    private void addRulesBefore(int priority, boolean forceContiguous, int insertBeforeRuleNumber, Integer... ruleNumbers) {
        addRulesBefore(priority, forceContiguous, Util.stream(getRulesSortedByPriority())
                .filter(r -> r.ruleArn().equals(RULE_NAME_PREFIX + insertBeforeRuleNumber)).findFirst(), ruleNumbers);
    }

    private void addRulesBefore(int priority, boolean forceContiguous, Optional<TestRuleAdapter> insertBefore, Integer... ruleNumbers) {
        ruleInserter.addRulesAssigningUnusedPriorities(forceContiguous, insertBefore,
                Util.map(Arrays.asList(ruleNumbers), ruleNumber->new TestRuleAdapter(/* isDefault */ false, priority, RULE_NAME_PREFIX + ruleNumber)));
    }
    
    private void assertRuleOrder(Integer... ruleNumbers) {
        assertEquals(Util.asList(Util.map(Arrays.asList(ruleNumbers), ruleNumber->RULE_NAME_PREFIX+ruleNumber)),
                // ignore default rule at index 0 in comparison; look at numbered rules only
                Util.asList(Util.map(getRulesSortedByPriority(), TestRuleAdapter::ruleArn)).subList(1, Util.size(getRulesSortedByPriority())));
    }

    private Iterable<TestRuleAdapter> getRulesSortedByPriority() {
        return ruleInserter.getRulesSortedByPriority();
    }
    
    private void assertUniqueAscendingPriorities() {
        int lastPriority = -1;
        for (final TestRuleAdapter rule : getRulesSortedByPriority()) {
            assertTrue(Integer.valueOf(rule.priority()) > lastPriority);
            lastPriority = Integer.valueOf(rule.priority());
        }
    }
}
