# How to use Secured Settings

We introduced a special type of settings, which are automatically check the accessebility of setting property by paywall resolver and the secured DTO in context.

## Usage

In the Settings class (e.g. ``RaceMapSettings.java``) the specific setting fields are implemented usually as one of the ``AbstractValueSetting<>`` classes (see common SSE project under ``com.sap.sse.common.settings.generic``). 

The initialization is done in the ``addChildSettings()`` method of an ``AbstractGenericSerializableSettings``, which is called before the constructor of the specific setting class is processed and before any field initialization in the setting class is done. Therefore, some potentially final fields like ``PaywallResolverProxy`` or ``SecuredDTOProxy`` are not declaired as final and are initialized in a lazy way to make sure they are always available for reading but will not be overwritten by another init step.

To enable security features directly when setting or reading a property, there are now other implementations of the value settings available (see SSE security UI project under ``com.sap.sse.security.ui.client.premium.settings``):
	
	SecuredBooleanSetting.java
	SecuredDecimalSetting.java
	SecuredDoubleSetting.java
	SecuredEnumSetting.java
	SecuredIntegerSetting.java
	SecuredLongSetting.java
	SecuredStringSetting.java
	SecuredUUIDSetting.java
	
They can be used instead of the 'traditional' settings with additional requirements: The PaywallResolver and a SecuredDTO have to be set.

Because often the settings have been defined before the respective secured DTO is available, the paywall resolver and the secured DTO are wrapped into proxy objects, so they can (have to be) set in a later time with ``.setPaywallResolver(paywallResolver)`` or ``.setSecuredDTO(securedDTO)``.

