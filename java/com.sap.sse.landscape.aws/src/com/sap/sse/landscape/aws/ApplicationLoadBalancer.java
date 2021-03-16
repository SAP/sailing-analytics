package com.sap.sse.landscape.aws;

import java.util.Optional;

import com.sap.sse.common.Named;
import com.sap.sse.landscape.Region;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.Listener;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.ProtocolEnum;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Rule;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.RuleCondition;

/**
 * Represents an AWS Application Load Balancer (ALB). When created, a default configuration with the following
 * attributes should be considered: {@code access_logs.s3.enabled==true}, then setting the {@code access_logs.s3.bucket}
 * and {@code access_logs.s3.prefix}, enabling {@code deletion_protection.enabled} and setting
 * {@code idle_timeout.timeout_seconds} to the maximum value of 4000s, furthermore spanning all availability
 * zones available in the region in which the ALB is deployed and using a specific security group that
 * allows for HTTP and HTTPS traffic.<p>
 * 
 * Furthermore, two listeners are always established: the HTTP listener forwards to a dedicated target group that
 * has as its target(s) the central reverse proxy/proxies. Any HTTP request arriving there will be re-written to
 * a corresponding HTTPS request and is then expected to arrive at the HTTPS listener of the same ALB.<p>
 * 
 * The HTTPS listener contains a default route that also forwards to central reverse proxy/proxies, requiring
 * another ALB-specific target group for HTTPS traffic.<p>
 * 
 * The two default rules and the two listeners are not entirely exposed by this interface. Instead, clients
 * will only see the non-default rule set of the HTTPS listener which is used to dynamically configure the
 * landscape.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ApplicationLoadBalancer<ShardingKey> extends Named {
    /**
     * The DNS name of this load balancer; can be used, e.g., to set a CNAME DNS record pointing
     * to this load balancer.
     */
    String getDNSName();

    Iterable<Rule> getRules();
    
    void deleteRules(Rule... rulesToDelete);

    Region getRegion();

    String getArn();
    
    default String getId() {
        return getArn().substring(getArn().lastIndexOf('/')+1);
    }

    /**
     * Application load balancer rules have a {@link Rule#priority() priority} which must be unique in the scope of a
     * load balancer's listener. This way, when rules come and go, holes in the priority numbering scheme will start to
     * exist. If a set of rules is to be added ({@code rulesToAdd}), consider using
     * {@link #addRulesAssigningUnusedPriorities} to make room for interleaved or contiguous addition of the new rules.
     * 
     * @param rulesToAdd
     *            rules (without an ARN set yet), specifying which rules to add to the HTTPS listener of this load
     *            balancer. All rules must have a priority that is not used currently by the listener.
     * @return the rules created, with ARNs set
     */
    Iterable<Rule> addRules(Rule... rulesToAdd);
    
    /**
     * As the rule {@link Rule#priority() priorities} within a load balancer's listener have to be unique, this method
     * supports adding rules by assigning yet unused priorities to them. It keeps the order in which the {@code rules}
     * are passed. If {@code forceContiguous} is {@code false}, for each rule the next available priority is chosen and
     * assigned by creating a copy of the {@link Rule} object and adding it to the resulting sequence. This can lead to
     * existing rules interleaving with the rules to add while ensuring that the {@code rules} have priorities in
     * numerically ascending order, consistent with the order in which they were passed to this method.
     * <p>
     * 
     * If {@code forceContiguous} is {@code true}, the rules that result will have contiguously increasing priority
     * values, hence not interleaving with other existing rules. If there are enough contiguous unused priorities
     * available, they are selected and assigned by creating copies of the {@link Rule} objects and adding them in their
     * original order to the resulting sequence. Otherwise, the existing rules are "compressed" by re-numbering their
     * priorities to make space for the new rules at the end of the list.
     * 
     * @return copies of the original rules, with unused {@link Rule#priority() priorities} assigned, as passed already
     *         to {@link #addRules(Rule...)}.
     */
    Iterable<Rule> addRulesAssigningUnusedPriorities(boolean forceContiguous, Rule... rules);
    
    Iterable<TargetGroup<ShardingKey>> getTargetGroups();

    /**
     * Deletes this application load balancer and all its {@link #getTargetGroups target groups}.
     */
    void delete() throws InterruptedException;

    void deleteListener(Listener listener);

    Listener getListener(ProtocolEnum protocol);

    /**
     * {@link #createDefaultRedirectRule(String, String, Optional) Creates} or updates a default re-direct rule in this
     * load balancer's HTTPS listener. Such a default re-direct rule is triggered by a request for the {@code hostname}
     * with the path being {@code "/"} and sends a re-direct response to the client that replaces path and query with
     * the values specified by the {@code path} and {@code query} parameters.
     * 
     * @return the {@link Rule} that represents the default re-direct
     */
    Rule setDefaultRedirect(String hostname, String path, Optional<String> query);

    RuleCondition createHostHeaderRuleCondition(String hostname);

    /**
     * Creates a new rule in the HTTPS listener of this load balancer. The rule fires when {@code "/"} is
     * the path ("empty" path) and the hostname header matches the value provided by the {@code hostname}
     * parameter. It sends a redirecting response with status code 302, redirecting to the same host, same
     * protocol and port and the path specified by the {@code pathWithLeadingSlash} parameter.<p>
     */
    Rule createDefaultRedirectRule(String hostname, String pathWithLeadingSlash, Optional<String> query);
}
