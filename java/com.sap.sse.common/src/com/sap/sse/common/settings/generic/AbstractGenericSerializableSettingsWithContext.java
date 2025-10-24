package com.sap.sse.common.settings.generic;

import java.util.HashMap;

/**
 * <i>See AbstractGenericSerializableSettings for further documentation.</i>
 * 
 * <p>
 * This is a special abstraction based on AbstractGenericSerializableSettings which provide support of a context object,
 * which can be used in addChildSettings method. This is needed because the addChildSettings method is called via super
 * constructor call and therefore, it is not possible to use any other data from outside (even not pre-initialized final
 * fields of the child object).
 * </p>
 * 
 * <p>
 * The main reason for this class was the initialization of secured settings in the addChildSettings method with secured
 * DTO and PaywallResolver.
 * </p>
 *
 */
public abstract class AbstractGenericSerializableSettingsWithContext<T> extends AbstractGenericSerializableSettings
        implements GenericSerializableSettings {
    private static final long serialVersionUID = -415371632234540296L;

    private final T context;

    /**
     * Default constructor for direct instantiation of root settings objects.
     * 
     * @param context
     *            a context object of type T (usually SecurityChildSettingsContext).
     */
    public AbstractGenericSerializableSettingsWithContext(T context) {
        super();
        this.context = context;
        addChildSettingsInternal(context);
    }

    /**
     * Constructor for automatic attachment of a child settings object to its parent settings object.
     * 
     * @param name
     *            the name of the child setting
     * @param settings
     *            the parent settings to attach this settings object to
     * @param context
     *            a context object of type T
     */
    public AbstractGenericSerializableSettingsWithContext(String name,
            AbstractGenericSerializableSettingsWithContext<T> settings, T context) {
        super(name, settings);
        this.context = context;
        addChildSettingsInternal(context);
    }

    protected final void addChildSettingsInternal(T context) {
        if (childSettings == null || childSettings.isEmpty()) {
            childSettings = new HashMap<>();
            addChildSettings(context);
        }
    }

    /**
     * This empty implementation is needed to use the AbstractGenericSerializableSettings but provide another
     * addChildSettings with context support. Therefore, the AbstractGenericSerializableSettings is extended but because
     * the addChildSettings is declared as abstract we want to hide this method.
     */
    @Override
    final protected void addChildSettings() {
    }

    /**
     * With the background of a special copy function, we provide access to the context object.
     * 
     * @return the context object.
     */
    public T getContext() {
        return context;
    }

    /**
     * Overwrite this method to initialize child settings. {@link AbstractGenericSerializableSettingsWithContext} for an
     * example.
     * 
     * TODO make abstract when all Settings are ported to the new system
     * 
     * @param context
     *            a context object of type T
     * 
     */
    protected abstract void addChildSettings(T context);

}
