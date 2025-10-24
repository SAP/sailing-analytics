package com.sap.sse.gwt.client.media;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.StringMessages;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.common.CommonSharedResources;
import com.sap.sse.gwt.common.CommonSharedResources.CommonMainCss;

/**
 * Use as a nested element of a parent {@code <div>} where the parent uses style {@link CommonMainCss#media_wrapper()}.
 * When using in a UI Binder {@code .ui.xml} file, use like this:
 * 
 * <pre>
 * &lt;ui:UiBinder xmlns:ui="urn:ui:com.google.gwt.uibinder"
 *                 xmlns:g="urn:import:com.google.gwt.user.client.ui"
 *                 ...
 *                 xmlns:m="urn:import:com.sap.sse.gwt.client.media"&gt;
 *  &ltui:with field="res" type="com.sap.sse.gwt.CommonSharedResources" /&gt;
 *  ...
 *  &lt;div ui:field="image" class="{local_res.css.countdown_image} {res.mainCss.media_wrapper}"&gt;
 *    &lt;m:MediaMenuIcon ui:field="imageMenuButton" fontSize="20px" /&gt;
 *  &lt;/div&gt;
 *  ...
 * &lt;/ui:UiBinder&gt;
 * </pre>
 * and in the UI Binder Java class declare the field for the {@code imageMenuButton} like this:
 * <pre>
 *   &commat;UiField(provided = true) MediaMenuIcon imageMenuButton;
 * </pre>
 * and in the UI Binder class constructor initialize like this:
 * <pre>
 *   this.imageMenuButton = new MediaMenuIcon(takedownNoticeService, "takedownRequestForImageOnEventStage");
 * </pre>
 * where the {@code takedownNotiveService} will typically be the {@code UserService} that may be available
 * from a {@code Presenter} field, and {@code "takedownRequestForImageOnEventStage"} as the message key
 * that resolves in the string messages of bundle {@code com.sap.sse.security} (see the {@code resources/stringmessages}
 * folder there).<p>
 * 
 * When you know the details of the media file to be displayed in the surrounding &lt;div&gt;, call
 * {@link #setData(String, String)} with the {@code contextDescriptionMessageParameter} and the {@code contentUrl}
 * parameters. The {@code contextDescriptionMessageParameter} will be used as a parameter for a single placeholder
 * expected in the message string identified by the {@code contextDescriptionMessageKey} parameter of the
 * constructor ({@code "takedownRequestForImageOnEventStage"} in the example above).
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class MediaMenuIcon extends Composite {

    private static final CommonMainCss MAIN_CSS = CommonSharedResources.INSTANCE.mainCss();

    private static MediaMenuIconUiBinder uiBinder = GWT.create(MediaMenuIconUiBinder.class);
    
    interface MediaMenuIconUiBinder extends UiBinder<Widget, MediaMenuIcon> {
    }

    @UiField HTML imageMenuButton;
    
    private final TakedownNoticeService takedownNoticeService;

    private final String contextDescriptionMessageKey;

    private String contextDescriptionMessageParameter;

    private String contentUrl;
    
    public MediaMenuIcon(TakedownNoticeService takedownNoticeService, String contextDescriptionMessageKey) {
        MAIN_CSS.ensureInjected();
        this.takedownNoticeService = takedownNoticeService;
        this.contextDescriptionMessageKey = contextDescriptionMessageKey;
        initWidget(uiBinder.createAndBindUi(this));
        imageMenuButton.addClickHandler(this::onClick);
    }
    
    public void setFontSize(String htmlFontSizeSpecification) {
        if (Util.hasLength(htmlFontSizeSpecification)) {
            final String valueString = htmlFontSizeSpecification.replaceFirst("[a-zA-Z]+$", "");
            final String unitString = htmlFontSizeSpecification.replaceFirst("^[0-9.]*", "");
            final double value = Double.valueOf(valueString);
            final Unit unit = Util.hasLength(unitString) ? Unit.valueOf(unitString.toUpperCase()) : Unit.PX;
            imageMenuButton.getElement().getStyle().setFontSize(value, unit);
        } else {
            GWT.log("Warning: unknown font size specification for MediaMenuIcon: "+htmlFontSizeSpecification);
        }
    }
    
    public void setZIndex(String zIndexSpecification) {
        if (Util.hasLength(zIndexSpecification)) {
            imageMenuButton.getElement().getStyle().setZIndex(Integer.parseInt(zIndexSpecification));
        }
    }
    
    public void setData(String contextDescriptionMessageParameter, String contentUrl) {
        this.contextDescriptionMessageParameter = contextDescriptionMessageParameter;
        this.contentUrl = contentUrl;
    }
    
    private void onClick(ClickEvent e) {
        e.stopPropagation();
        e.preventDefault();
        if (takedownNoticeService.isEmailAddressOfCurrentUserValidated()) {
            final TakedownNoticeRequestDialog dialog = new TakedownNoticeRequestDialog(contextDescriptionMessageKey, contextDescriptionMessageParameter, contentUrl,
                    takedownNoticeService.getCurrentUserName(), StringMessages.INSTANCE,
                    new DialogCallback<TakedownNoticeRequestContext>() {
                @Override
                public void ok(TakedownNoticeRequestContext editedObject) {
                    takedownNoticeService.fileTakedownNotice(editedObject);
                }
                
                @Override
                public void cancel() {
                }
            });
            dialog.show();
         } else {
             Notification.notify(StringMessages.INSTANCE.mustBeLoggedInAndWithValidatedEmail(), NotificationType.ERROR);
         }
    }
}
