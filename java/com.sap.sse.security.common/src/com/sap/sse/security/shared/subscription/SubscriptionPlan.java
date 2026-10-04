package com.sap.sse.security.shared.subscription;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sap.sse.security.shared.impl.Role;
import com.sap.sse.security.shared.impl.User;

/**
 * Payment subscription plans. A subscription plan has a name, a {@link String}-based ID, and a set of
 * {@link SubscriptionPlanRole roles} it grants to a subscribing user. These roles can specify how they are to be
 * qualified when assigned, regarding user and group qualifications. See
 * {@link SubscriptionPlanRole.GroupQualificationMode} and {@link SubscriptionPlanRole.UserQualificationMode} for
 * more details.
 * 
 * @author Tu Tran
 */
public abstract class SubscriptionPlan implements Serializable {
    private static final long serialVersionUID = -555811806344107292L;
    private final String id;
    private final Set<SubscriptionPrice> prices;
    private final Set<PlanCategory> planCategory;
    private final Boolean isOneTimePlan;
    private final PlanGroup group;
    /**
     * Roles assigned for this plan, if user subscribe to the plan then the user will be assigned these roles
     */
    private final SubscriptionPlanRole[] roles;
    
    /**
     * Used to make Plans of the same category mutually exclusive.
     * BASIC is a dummy category used to define the features in the feature list.
     */
    public enum PlanCategory {
        NOT_LOGGED_IN("not_logged_in",
                "features_limited_live_analytics"),
        BASIC("free_subscription_plan",
                "features_notifications",
                "features_organize_events",
                "features_events_with_more_regatta",
                "features_connect_to_tractrac",
                "features_imports",
                "features_media_management",
                "features_limited_live_analytics",
                "features_media_tags",
                "features_scoring"),
        PREMIUM("premium",
                "features_notifications",
                "features_organize_events",
                "features_events_with_more_regatta",
                "features_connect_to_tractrac",
                "features_imports",
                "features_media_management",
                "features_limited_live_analytics",
                "features_media_tags",
                "features_scoring",
                "features_wind_analytics",
                "features_maneuver_analytics",
                "features_competitor_analytics",
                "features_advanced_leaderboard_info",
                "features_simulator",
                "features_map_analytics"),
        DATA_MINING_ARCHIVE("data_mining_archive",
                "features_notifications",
                "features_organize_events",
                "features_events_with_more_regatta",
                "features_connect_to_tractrac",
                "features_imports",
                "features_media_management",
                "features_limited_live_analytics",
                "features_media_tags",
                "features_scoring",
                "features_wind_analytics",
                "features_maneuver_analytics",
                "features_competitor_analytics",
                "features_advanced_leaderboard_info",
                "features_simulator",
                "features_map_analytics",
                "features_data_mining"),
        DATA_MINING_ALL("data_mining_all",
                "features_notifications",
                "features_organize_events",
                "features_events_with_more_regatta",
                "features_connect_to_tractrac",
                "features_imports",
                "features_media_management",
                "features_limited_live_analytics",
                "features_media_tags",
                "features_scoring",
                "features_wind_analytics",
                "features_maneuver_analytics",
                "features_competitor_analytics",
                "features_advanced_leaderboard_info",
                "features_simulator",
                "features_map_analytics",
                "features_data_mining",
                "features_data_mining_all"),
        TRIAL("trial");
        
        final String id;
        
        final Set<String> featureIds;
        
        PlanCategory(String id, String...featureIds) {
            this.id = id;
            // Use LinkedHashSet to preserve the declaration order of the feature ids. This order is relied upon
            // when rendering the feature list in the UI. A plain HashSet happened to yield the same order due to
            // how GWT translates it to JavaScript, but that was coincidental; LinkedHashSet makes it explicit and
            // robust against changes in GWT's HashSet implementation.
            this.featureIds = new LinkedHashSet<>(Arrays.asList(featureIds));
        }
        
        public String getId() {
            return id;
        }
        
        public boolean containsFeature(String featureId) {
            return featureIds.contains(featureId);
        }
        
        private boolean hasFeature() {
            return !featureIds.isEmpty();
        }
        
        public static List<PlanCategory> getCategoriesWithFeature() {
            return Stream.of(PlanCategory.values())
                    .filter(c -> c.hasFeature())
                    .collect(Collectors.toList());
        }
        
        public static Set<String> getAllFeatureIds() {
            final Set<String> featureIds = new LinkedHashSet<>();
            for (final PlanCategory category : PlanCategory.values()) {
                featureIds.addAll(category.featureIds);
            }
            return featureIds;
        }
    }
    
    /**
     * Used to organize plans in groups to show on one subscription card. 
     * The order of this enumeration is usually also the main order on the UI.
     */
    public enum PlanGroup {
        PREMIUM("premium"), DATA_MINING_ARCHIVE("data_mining_archive"), DATA_MINING_ALL("data_mining_all"), TRIAL("trial");
        final String id;
        PlanGroup(String id) {
            this.id = id;
        }
        public String getId() {
            return id;
        }
    }
    
    protected SubscriptionPlan(String id, Set<SubscriptionPrice> prices, Set<PlanCategory> planCategory,
            Boolean isOneTimePlan, PlanGroup group, SubscriptionPlanRole[] roles) {
        this.id = id;
        this.roles = roles;
        this.prices = prices;
        this.planCategory = planCategory;
        this.isOneTimePlan = isOneTimePlan;
        this.group = group;
    }

    public String getId() {
        return id;
    }

    public Set<SubscriptionPrice> getPrices() {
        return prices;
    }

    public SubscriptionPlanRole[] getRoles() {
        return roles;
    }
    
    public Set<PlanCategory> getPlanCategories() {
        return planCategory;
    }
    
    public Boolean getIsOneTimePlan() {
        return isOneTimePlan;
    }
    
    public PlanGroup getGroup() {
        return group;
    }

    public boolean isUserInPossessionOfRoles(User user) {
        boolean foundAll = true;
        for (SubscriptionPlanRole planRole : getRoles()) {
            boolean found = false;
            for (Role userRole : user.getRoles()) {
                if (userRole.getRoleDefinition().getId().equals(planRole.getRoleId())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                foundAll = false;
                break;
            }
        }
        return foundAll;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + Arrays.hashCode(roles);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SubscriptionPlan other = (SubscriptionPlan) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (!Arrays.equals(roles, other.roles))
            return false;
        return true;
    }
}
