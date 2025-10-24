package com.sap.sailing.selenium.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Locatable;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;
import org.openqa.selenium.support.pagefactory.FieldDecorator;
import org.openqa.selenium.support.pagefactory.internal.LocatingElementHandler;
import org.openqa.selenium.support.pagefactory.internal.LocatingElementListHandler;

public class SeleniumFieldDecorator implements FieldDecorator {
    private ElementLocatorFactory factory;
    
    public SeleniumFieldDecorator(ElementLocatorFactory factory) {
        this.factory = factory;
    }
    
    @Override
    public Object decorate(ClassLoader loader, Field field) {
        ElementLocator locator = this.factory.createLocator(field);
        
        if (locator == null) {
            return null;
        }
        
        if (isDecoratableField(field)) {
            return proxyForElementLocator(loader, locator);
        }
        
        if (isDecoratableList(field)) {
            return proxyForElementsLocator(loader, locator);
        }
        
        return null;
    }
    
    private boolean isDecoratableField(Field field) {
        return WebElement.class.isAssignableFrom(field.getType());
    }
    
    private boolean isDecoratableList(Field field) {
        if (!List.class.isAssignableFrom(field.getType())) {
          return false;
        }
        
        // Type erasure in Java isn't complete. Attempt to discover the generic type of the list.
        Type genericType = field.getGenericType();
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }
        
        Type listType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        
        if (!WebElement.class.equals(listType)) {
            return false;
        }
    
        return true;
      }
    
    private Object proxyForElementLocator(ClassLoader loader, ElementLocator locator) {
        InvocationHandler handler = new LocatingElementHandler(locator);
        Object proxy = Proxy.newProxyInstance( loader, new Class[] {WebElement.class, WrapsElement.class, Locatable.class}, handler);
        
        return proxy;
    }

    private Object proxyForElementsLocator(ClassLoader loader, ElementLocator locator) {
        InvocationHandler handler = new LocatingElementListHandler(locator);
        Object proxy = Proxy.newProxyInstance(loader, new Class[] {List.class}, handler);
        
        return proxy;
    }
}
