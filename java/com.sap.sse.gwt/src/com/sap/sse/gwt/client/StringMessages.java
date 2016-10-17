package com.sap.sse.gwt.client;

import com.google.gwt.i18n.client.Messages;

/**
 * Defines the text strings for i18n that are used by the SSE GWT bundle. 
 * @author Axel Uhl (D043530)
 *
 */
public interface StringMessages extends Messages {
    String save();
    String remove();
    String ok();
    String cancel();
    String add();
    String edit();
    String serverError(); 
    String close();
    String remoteProcedureCall();
    String serverReplies();
    String errorCommunicatingWithServer();
    String configuration();
    String settings();
    String settingsForComponent(String localizedShortName);
    String pleaseSelect();
    //TODO add to properties files
    @DefaultMessage("Make default")
    String makeDefault();
}
