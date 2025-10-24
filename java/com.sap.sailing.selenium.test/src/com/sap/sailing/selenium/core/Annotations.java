package com.sap.sailing.selenium.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.openqa.selenium.By;
import org.openqa.selenium.support.CacheLookup;
import org.openqa.selenium.support.pagefactory.ByChained;

/**
 * <p>Builder which is responsible for the creation of location mechanism according to the annotations applied to a
 *   field. The builder supports the annotations {@link org.openqa.selenium.support.CacheLookup}, {@link FindBy} and
 *   {@link FindBys}.</p>
 * 
 * @author
 *   D049941
 */
public class Annotations {
    /**
     * <p>Checks if the given field is annotated with one of the annotations supported by the builder.</p>
     * 
     * @param field
     *   The field to check for the annotations.
     * @return
     *   {@code true} if the field is annotated with one of the supported annotations and {@code false} otherwise.
     */
    public static boolean isAnnotationPresent(Field field) {
        return (field.getAnnotation(FindBys.class) != null || field.getAnnotation(FindBy.class) != null);
    }

    private Field field;

    /**
     * <p>Creates a new builder for the given field.</p>
     * 
     * @param field
     *   The field for which the location mechanism is to build.
     */
    public Annotations(Field field) {
        this.field = field;
    }

    /**
     * <p>Determines if the field is annotated with {@code CacheLookup} to indicate that it never changes (that is, that
     *   the same instance in the DOM will always be used).</p>
     * 
     * @return
     *   {@code true} if the field is annotated with {@code CacheLookup} and {@code false} otherwise.
     */
    public boolean isLookupCached() {
        return (this.field.getAnnotation(CacheLookup.class) != null);
    }

    /**
     * <p>Builds and returns the location mechanism for the field according to the defined annotations.</p>
     * 
     * @return
     *   The location mechanism for the field.
     */
    public By buildBy() {
        assertValidAnnotations();

        FindBys findBys = this.field.getAnnotation(FindBys.class);
        if (findBys != null) {
            return buildByFromFindBys(findBys);
        }

        FindBy findBy = this.field.getAnnotation(FindBy.class);
        if (findBy != null) {
            return buildByFromFindBy(findBy);
        }

        throw new IllegalArgumentException("Cannot determine how to locate element " + this.field); //$NON-NLS-1$
    }

    /**
     * <p>Builds and returns the location mechanism as specified by the given {@code FindBy}.</p>
     * 
     * @param findBy
     *   The definition of the location mechanism.
     * @return
     *   The location mechanism as specified.
     */
    protected By buildByFromFindBy(FindBy findBy) {
        assertValidFindBy(findBy);

        try {
            Class<? extends By> how = findBy.how();
            Constructor<? extends By> constructor = how.getConstructor(String.class);
            String using = findBy.using();

            return constructor.newInstance(using);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * <p>Builds and returns the location mechanism as specified by the given {@code FindBys}.</p>
     * 
     * @param findBys
     *   The definition of the location mechanism.
     * @return
     *   The location mechanism as specified.
     */
    protected By buildByFromFindBys(FindBys findBys) {
        assertValidFindBys(findBys);

        FindBy[] value = findBys.value();
        By[] bys = new By[value.length];
        
        for (int i = 0; i < value.length; i++) {
            bys[i] = buildByFromFindBy(value[i]);
        }

        return new ByChained(bys);
    }

    private void assertValidAnnotations() {
        int count = 0;

        if (this.field.getAnnotation(FindBys.class) != null) {
            count += 1;
        }

        if (this.field.getAnnotation(FindBy.class) != null) {
            count += 1;
        }

        if (count > 1) {
            throw new IllegalArgumentException("You may use only one of '@FindBy' and '@FindBys' annotations"); //$NON-NLS-1$
        }
    }

    private void assertValidFindBys(FindBys findBys) {
        for (FindBy findBy : findBys.value()) {
            assertValidFindBy(findBy);
        }
    }

    private void assertValidFindBy(FindBy findBy) {
        if (findBy.how() == null || findBy.using() == null) {
            throw new IllegalArgumentException("You must define the 'how' property as well as the 'using' property"); //$NON-NLS-1$
        }
    }

}
