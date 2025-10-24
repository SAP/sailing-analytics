package com.sap.sse.common.settings.generic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.sap.sse.common.settings.GwtIncompatible;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.common.settings.value.SettingsValue;
import com.sap.sse.common.settings.value.Value;

/**
 * Common superclass for custom Settings used by Components. The following example shows how to implement custom
 * Settings:
 * 
 * <pre>
 * <code>
 * public final class ExampleSettings extends AbstractGenericSerializableSettings {
 *     private transient StringSetting myString;
 *     private transient BooleanSetting myBoolean;
 *     
 *     // Used to create instances for a component
 *     public ExampleSettings() {
 *     }
 *     
 *     // Used to create nested settings
 *     public ExampleSettings(String name, AbstractSettings parentSettings) {
 *         super(name, parentSettings);
 *     }
 *     
 *     &commat;Override
 *     protected void addChildSettings() {
 *         myString = new UUIDSetting("myString", this)
 *         myBoolean = new BooleanSetting("myBoolean", this);
 *     }
 *     
 *     public String getMyString() {
 *         return this.myString.getValue();
 *     }
 *     
 *     public void setMyString(String myString) {
 *         this.myString.setValue(myString);
 *     }
 *     
 *     public boolean isMyBoolean() {
 *         return this.myBoolean.getValue();
 *     }
 *     
 *     public void setMyBoolean(boolean myBoolean) {
 *         this.myBoolean.setValue(myBoolean);
 *     }
 * }
 * </code>
 * </pre>
 * 
 * To correctly support several kinds of Serialization (Java, GWT and Custom), you need to ensure the following:
 * <ul>
 *   <li>the Settings class must have a default constructor</li>
 *   <li>child settings fields need to be transient</li>
 *   <li>child settings instances must be initialized in {@link #addChildSettings()} method</li>
 * </ul>
 * 
 * If a custom Settings class only has {@link Setting} oder {@link Settings} children that are correctly attached by
 * calling their constructor with a name and the parent {@link Settings} you do not need to implement
 * {@link #hashCode()}, {@link #equals(Object)} and {@link #toString()}. There are generic implementations of those methods that are
 * based on the child {@link Setting}s which should be sufficient for most cases.
 *
 */
public abstract class AbstractGenericSerializableSettings extends AbstractSetting implements GenericSerializableSettings {
    private static final long serialVersionUID = -415371632804540785L;
    protected SettingsValue value;
    protected transient Map<String, Setting> childSettings;

    /**
     * Default constructor for direct instantiation of root settings objects.
     */
    public AbstractGenericSerializableSettings() {
        super();
        value = new SettingsValue();
        addChildSettingsInternal();
    }

    /**
     * Constructor for automatic attachment of a child settings object to its parent settings object.
     * 
     * @param name the name of the child setting
     * @param settings the parent settings to attach this settings object to
     */
    public AbstractGenericSerializableSettings(String name, AbstractGenericSerializableSettings settings) {
        super(name, settings);
        value = (SettingsValue) settings.getValue(name);
        if (value == null) {
            value = new SettingsValue();
            settings.setValue(name, value);
        }
        addChildSettingsInternal();
    }
    
    /**
     * Internal use only for correct lifecycle of {@link SettingsList}.
     */
    protected void adoptValue(SettingsValue value) {
        this.value = value;
        if (childSettings != null) {
            for (Map.Entry<String, Setting> entry : childSettings.entrySet()) {
                Setting childSetting = entry.getValue();
                if (childSetting instanceof AbstractGenericSerializableSettings) {
                    Value childValue = value.getValue(entry.getKey());
                    if (childValue != null) {
                        ((AbstractGenericSerializableSettings) childSetting).adoptValue((SettingsValue) childValue);
                    }
                }
                if (childSetting instanceof SettingsList<?>) {
                    Value childValue = value.getValue(entry.getKey());
                    if (childValue != null) {
                        ((SettingsList<?>) childSetting).adoptValue();
                    }
                }
            }
        }
    }
    
    protected SettingsValue getInnerValueObject() {
        return value;
    }
    
    protected final void addChildSettingsInternal() {
        if (childSettings == null) {
            childSettings = new HashMap<>();
            addChildSettings();
        }
    }
    
    /**
     * Overwrite this method to initialize child settings.
     * {@link AbstractGenericSerializableSettings} for an example.
     * 
     * TODO make abstract when all Settings are ported to the new system
     * 
     */
    protected abstract void addChildSettings();

    public Value getValue(String settingName) {
        return value.getValue(settingName);
    }

    public void setValue(String settingName, Value value) {
        this.value.setValue(settingName, value);
    }

    protected void addSetting(String name, Setting setting) {
        if (name.contains(GenericSerializableSettings.PATH_SEPARATOR)) {
            throw new IllegalArgumentException("\"" + GenericSerializableSettings.PATH_SEPARATOR
                    + "\" is currently not allowed for setting names");
        }
        if (childSettings.containsKey(name)) {
            throw new IllegalArgumentException("setting with name " + name + " already added to setting type "
                    + getClass().getSimpleName());
        }
        childSettings.put(name, setting);
    }

    @Override
    public boolean isDefaultValue() {
        for (Setting setting : childSettings.values()) {
            if (!setting.isDefaultValue()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void resetToDefault() {
        for (Setting setting : childSettings.values()) {
            setting.resetToDefault();
        }
    }

    @Override
    public Map<String, Setting> getChildSettings() {
        synchronized(childSettings) {
            return new HashMap<String, Setting>(childSettings);
        }
    }
    
    @GwtIncompatible
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        addChildSettingsInternal();
    }
    
    @GwtIncompatible
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("{(").append(this.getClass().getSimpleName()).append(")");
        boolean first = true;
        for (Map.Entry<String, Setting> childSetting : childSettings.entrySet()) {
            if (!first) {
                sb.append("; ");
            }
            sb.append(childSetting.getKey() + "=" + childSetting.getValue());
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((childSettings == null) ? 0 : childSettings.hashCode());
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
        AbstractGenericSerializableSettings other = (AbstractGenericSerializableSettings) obj;
        if (childSettings == null) {
            if (other.childSettings != null)
                return false;
        } else if (!childSettings.equals(other.childSettings))
            return false;
        return true;
    }
}
