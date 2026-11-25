package com.sap.sailing.selenium.pages.gwt;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * <p></p>
 * 
 * @author
 *   D049941
 * @param <T>
 *   The type of the data entries the table contains.
 */
public class GenericCellTablePO<T extends DataEntryPO> extends CellTablePO<T> {
    private static final Logger logger = Logger.getLogger(GenericCellTablePO.class.getName());

    /**
     * <p></p>
     * 
     * @author
     *   D049941
     * @param <T>
     *   The type of the data entries the factory creates.
     */
    public static interface DataEntryFactory<T extends DataEntryPO> {
        /**
         * <p>Creates and returns a new entry which represents the specified element (a row) of the given table.</p>
         * 
         * @param table
         *   The table containing the entry.
         * @param element
         *   The underlying element represented by the entry to create.
         * @return
         *   The data entry representing the element.
         */
        public <S extends CellTablePO<T>> T createEntry(S table, WebElement element);
    }
    
    /**
     * <p>Default implementation of the interface DataEntryFactory which use reflection to create the data entries.</p>
     * 
     * @author
     *   D049941
     * @param <T>
     *   The type of the data entries the factory creates.
     */
    public static class DefaultDataEntryFactory<T extends DataEntryPO> implements DataEntryFactory<T> {
        private Class<T> type;
        
        /**
         * <p>Creates a new factory for the specified type of data entries.</p>
         * 
         * @param type
         *   The class object of the entries to create.
         */
        public DefaultDataEntryFactory(Class<T> type) {
            this.type = type;
        }
        
        /**
         * TODO [D049941]
         * 
         * To be able to create entries of a specified type, the type must have a public constructor with two arguments
         * with the formal parameter types CellTable and WebElement.</p>
         */
        @Override
        public <S extends CellTablePO<T>> T createEntry(S table, WebElement element) {
            Class<?> clazz = table.getClass();
            T result = null;
            final List<Exception> exceptionsCaught = new ArrayList<>();
            while (clazz != null && result == null) {
                try {
                    final Constructor<T> constructor = this.type.getConstructor(clazz, WebElement.class);
                    result = constructor.newInstance(table, element);
                } catch (final NoSuchMethodException exception) {
                    clazz = clazz.getSuperclass();
                    exceptionsCaught.add(exception);
                } catch (final InvocationTargetException exception) {
                    if (exception.getCause() instanceof StaleElementReferenceException) {
                        throw (StaleElementReferenceException) exception.getCause();
                    } else {
                        throw new RuntimeException("Can't create DataEntry of type " + this.type
                                + " on table of type " + table.getClass().getName() + " on web element " + element,
                                exception);
                    }
                } catch (final InstantiationException | IllegalAccessException exception) {
                    throw new RuntimeException("Can't create DataEntry of type " + this.type + " on table of type "
                            + table.getClass().getName() + " on web element " + element, exception);
                }
            }
            if (result == null) {
                logger.warning("Unable to construct a DataEntryPO of type " + this.type.getName() + " for table "
                        + table + " of type " + (table == null ? null : table.getClass().getName())
                        + " from web element " + element);
                for (final Exception e : exceptionsCaught) {
                    logger.log(Level.WARNING,
                            "Exception caught while trying to create a DataEntryPO of type " + this.type.getName(), e);
                }
                throw new RuntimeException("Can't create DataEntry of type " + this.type + " on table of type "
                        + table.getClass().getName() + " on web element " + element);
            }
            return result;
        }
    }
    
    private DataEntryFactory<T> factory;
    
    public GenericCellTablePO(WebDriver driver, WebElement element, Class<T> entryType) {
        this(driver, element, new DefaultDataEntryFactory<>(entryType));
    }
    
    public GenericCellTablePO(WebDriver driver, WebElement element, DataEntryFactory<T> factory) {
        super(driver, element);
        
        this.factory = factory;
    }
    
    @Override
    protected T createDataEntry(WebElement element) {
        return this.factory.createEntry(this, element);
    }
}
