package com.sap.sailing.gwt.home.client.shared.solutions;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public interface SolutionsResources extends ClientBundle {
    public static final SolutionsResources INSTANCE = GWT.create(SolutionsResources.class);

    @Source("com/sap/sailing/gwt/home/client/shared/solutions/Solutions.css")
    LocalCss css();

    public interface LocalCss extends CssResource {
        String solutions();
        String solutions_nav();
        String solutions_nav_link();
        String solutions_content();
        String parallax();
        String grid();
        String solutions_contentsap();
        String solutions_contentrace();
        String solutions_contentpost();
        String solutions_contenttraining();
        String solutions_contentsimulator();
        String solutions_content_linkappstore();
    }
}
