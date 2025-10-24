package com.sap.sailing.gwt.home.desktop.places.user.profile.subscriptionstab;

import static com.sap.sse.gwt.client.DateAndTimeFormatterUtil.formatDateAndTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.SafeHtmlCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.SimpleSafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DefaultCellTableBuilder;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.theme.component.celltable.DesignedCellTableResources;
import com.sap.sailing.gwt.home.desktop.places.user.profile.UserProfileResources;
import com.sap.sailing.gwt.home.desktop.places.user.profile.subscriptionstab.UserProfileSubscriptionsResources.SubscriptionProfileCss;
import com.sap.sailing.gwt.home.shared.places.user.profile.subscriptions.UserSubscriptionsView;
import com.sap.sailing.gwt.home.shared.places.user.subscriptions.SubscriptionsValueProvider;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.TimePoint;
import com.sap.sse.gwt.client.celltable.SortedCellTable;
import com.sap.sse.security.ui.shared.subscription.SubscriptionDTO;
import com.sap.sse.security.ui.shared.subscription.SubscriptionListDTO;

/**
 * Implementation view for {@link UserSubscriptionsView}
 */
public class UserSubscriptions extends Composite implements UserSubscriptionsView {

    interface MyUiBinder extends UiBinder<Widget, UserSubscriptions> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    @UiField
    StringMessages i18n;
    @UiField
    UserProfileSubscriptionsResources local_res;

    @UiField
    Button subscribeButtonUi;
    @UiField
    Button selfServiceControlUi;
    @UiField(provided = true)
    SortedCellTable<SubscriptionDTO> subscriptionsUi = new SortedCellTable<>(0, DesignedCellTableResources.INSTANCE);

    private final Presenter presenter;
    private final SubscriptionsValueProvider valueProvider;

    public UserSubscriptions(final UserSubscriptionsView.Presenter presenter) {
        initWidget(uiBinder.createAndBindUi(this));
        local_res.css().ensureInjected();
        this.presenter = presenter;
        this.presenter.setView(this);
        this.valueProvider = new SubscriptionsValueProvider(i18n);
        initSubscriptionsTable(presenter);
    }
    
    @UiHandler("subscribeButtonUi")
    public void onSubscribeClicked(final ClickEvent event) {
        presenter.navigateToSubscribe();
    }
    
    @UiHandler("selfServiceControlUi")
    void onSelfServiceControlClicked(final ClickEvent event) {
        presenter.openSelfServicePortal();
    }
    
    @Override
    public void updateView(final SubscriptionListDTO subscriptions) {
        subscribeButtonUi.setEnabled(true);
        if (subscriptions == null) {
            selfServiceControlUi.setVisible(false);
            subscriptionsUi.setPageSize(0);
            subscriptionsUi.setList(new ArrayList<SubscriptionDTO>());
        } else {
            selfServiceControlUi.setVisible(true);
            subscriptionsUi.setPageSize(subscriptions.getSubscriptionItems().length);
            subscriptionsUi.setList(Arrays.asList(subscriptions.getSubscriptionItems()));
        }
        setVisible(true);
    }
    
    private void initSubscriptionsTable(final Presenter presenter) {
        subscriptionsUi.setRowStyles((row, index) -> local_res.css().defaultTableRow());
        final Column<SubscriptionDTO, ?> nameColumn = new TextColumn<SubscriptionDTO>() {
            @Override
            public String getValue(final SubscriptionDTO object) {
                return valueProvider.getSubscriptionName(object);
            }
        };
        nameColumn.setCellStyleNames(local_res.css().fontWeightBold());
        subscriptionsUi.addColumn(nameColumn, i18n.name());
        subscriptionsUi.addColumn(new Column<SubscriptionDTO, SafeHtml>(new SafeHtmlCell()) {
            @Override
            public SafeHtml getValue(final SubscriptionDTO object) {
                return valueProvider.getSubscriptionStatusLabel(object);
            }
        }, i18n.status());
        addDateTimeColumn(i18n.createdAt(), SubscriptionDTO::getCreatedAt);
        addDateTimeColumn(i18n.currentTermEnd(), valueProvider::getTermEnd);
        final Column<SubscriptionDTO, String> cancelColumn = new Column<SubscriptionDTO, String>(new ButtonCell()) {
            @Override
            public void render(final Context context, final SubscriptionDTO object, final SafeHtmlBuilder sb) {
                final UnaryOperator<SafeHtml> template = object.isCancelled() || !object.isRenewing()
                        ? UserProfileResources.TEMPLATE::disabledButtonCell
                        : UserProfileResources.TEMPLATE::removeButtonCell;
                sb.append(template.apply(SimpleSafeHtmlRenderer.getInstance().render(getValue(object))));
            }
            @Override
            public String getValue(final SubscriptionDTO object) {
                return i18n.cancelSubscription();
            }
        };
        cancelColumn.setCellStyleNames(DesignedCellTableResources.INSTANCE.cellTableStyle().buttonCell());
        cancelColumn.setFieldUpdater(new FieldUpdater<SubscriptionDTO, String>() {
            @Override
            public void update(final int index, final SubscriptionDTO object, final String value) {
                if (!object.isCancelled()) {
                    presenter.nonRenewingSubscription(object.getSubscriptionPlanId(), object.getProvider());
                }
            }
        });
        subscriptionsUi.addColumn(cancelColumn);
        subscriptionsUi.setTableBuilder(new DefaultCellTableBuilder<SubscriptionDTO>(subscriptionsUi) {
            @Override
            public void buildRowImpl(final SubscriptionDTO rowValue, final int absRowIndex) {
                super.buildRowImpl(rowValue, absRowIndex);
                final SubscriptionProfileCss css = local_res.css();
                final TableRowBuilder tr = startRow();
                tr.className(local_res.css().borderTableRow());
                addTextCell(tr, i18n.paymentStatus() + ":", css.fontStyleItalic());
                valueProvider.configurePaymentStatusElement(rowValue, (label, value) -> addTextCell(tr, value),
                        (label, value) -> addImageCell(tr, value));
                if (!rowValue.isInTrial() && (rowValue.isCancelled() || !rowValue.isRenewing())) {
                    addTextCell(tr, i18n.cancelledAt() + ":", css.textAlignRight());
                    addTextCell(tr, formatDateAndTime(rowValue.getCancelledAt().asDate()), css.textAlignRight());
                } else if (rowValue.isRenewing()) {
                    addTextCell(tr, i18n.nextBillingAt() + ":", css.fontStyleItalic(), css.textAlignRight());
                    addTextCell(tr, formatDateAndTime(rowValue.getNextBillingAt().asDate()), css.textAlignRight());
                    addTextCell(tr, valueProvider.getRecurringPayment(rowValue), css.textAlignRight());
                }
                tr.endTR();
            }
            private void addTextCell(final TableRowBuilder tr, final String text, final String... classNames) {
                final StringJoiner styles = new StringJoiner(" ");
                Stream.of(classNames).forEach(styles::add);
                final TableCellBuilder td = tr.startTD().className(cellTable.getResources().style().cell());
                td.startDiv().className(styles.toString()).text(text).endDiv();
                td.endTD();
            }
            private void addImageCell(final TableRowBuilder tr, final DataResource image) {
                final TableCellBuilder td = tr.startTD().className(cellTable.getResources().style().cell());
                td.startImage().src(image.getSafeUri().asString()).width(24).endImage();
                td.endTD();
            }
        });
    }

    private void addDateTimeColumn(final String header, final Function<SubscriptionDTO, TimePoint> valueProvider) {
        final Column<SubscriptionDTO, ?> dateTimeColumn = new TextColumn<SubscriptionDTO>() {
            @Override
            public String getValue(final SubscriptionDTO object) {
                final TimePoint timePoint = valueProvider.apply(object);
                return formatDateAndTime(timePoint.asDate());
            }
        };
        dateTimeColumn.setCellStyleNames(local_res.css().textAlignRight());
        final Header<?> dateTimeHeader = new TextHeader(header);
        dateTimeHeader.setHeaderStyleNames(local_res.css().textAlignRight());
        subscriptionsUi.addColumn(dateTimeColumn, dateTimeHeader);
    }

}
