package com.sap.sailing.selenium.test.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.common.DataEntryDialogPO;
import com.sap.sailing.selenium.pages.gwt.CheckBoxPO;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

/** Native-browser regressions for interaction boundaries; no SailingServer or database is required. */
public class TestPageObjectInteractions extends AbstractSeleniumTest {
    @Override
    @BeforeEach
    public void setUp() {
        try (final InputStream fixture = getClass().getResourceAsStream("interactions.html")) {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final byte[] buffer = new byte[4096];
            int count;
            while ((count = fixture.read(buffer)) != -1) {
                bytes.write(buffer, /* off */ 0, count);
            }
            getWebDriver().get("data:text/html;charset=utf-8;base64,"
                    + Base64.getEncoder().encodeToString(bytes.toByteArray()));
        } catch (final IOException e) {
            throw new IllegalStateException("Cannot load interaction fixture", e);
        }
    }

    @SeleniumTestCase
    public void selectsClippedRowAfterReplacementWithoutActivatingItsLink() {
        final GenericCellTablePO<DataEntryPO> table = table();
        final DataEntryPO row = entry(table, "R3");
        script("window.replaceOnScroll = true; showOverlay();");
        row.select();
        assertSelection(table, "R3");
        assertTrue(((Number) script("return replacements;")).intValue() >= 2);
        assertEquals(/* expected */ 1L, script("return rowClicks;"));
        assertNativeClicks();
    }

    @SeleniumTestCase
    public void keepsControlSelectionAcrossReplacedAndReorderedRows() {
        final GenericCellTablePO<DataEntryPO> table = table();
        entry(table, "R1").select();
        table.selectEntries(() -> table.getEntries().stream()
                .filter((final DataEntryPO row) -> !"R2".equals(row.getColumnContent("Name"))));
        assertSelection(table, "R1", "R3");
        entry(table, "R3").appendToSelection();
        assertEquals(/* expected */ 4L, script("return rowClicks;"));
        entry(table, "R1").deselect();
        assertSelection(table, "R3");
        // An ordinary click replaces the selection, proving that Control was released.
        entry(table, "R2").select();
        assertSelection(table, "R2");
        assertEquals(/* expected */ 6L, script("return rowClicks;"));
        assertNativeClicks();
    }

    @SeleniumTestCase
    public void evaluatesSelectionPredicateOnceBeforeRowsAreReplaced() {
        final GenericCellTablePO<DataEntryPO> table = table();
        final AtomicInteger evaluations = new AtomicInteger();
        table.selectEntries((final DataEntryPO row) -> evaluations.incrementAndGet() % 2 == 1, () -> false);
        assertEquals(/* expected */ 3, evaluations.get());
        assertSelection(table, "R1", "R3");
        assertEquals(/* expected */ 2L, script("return rowClicks;"));
        assertNativeClicks();
    }

    @SeleniumTestCase
    public void keepsCheckboxSelectionAcrossReplacedRows() {
        script("window.checkboxMode = true; render();");
        assertCheckboxSelection();
    }

    @SeleniumTestCase
    public void recognizesSelectAllCheckboxHeaderWithGwtCheckboxCells() {
        script("window.checkboxMode = true; window.checkboxHeader = true; render();");
        assertCheckboxSelection();
    }

    private void assertCheckboxSelection() {
        final GenericCellTablePO<DataEntryPO> table = table();
        table.selectEntries(() -> table.getEntries().stream());
        assertSelection(table, "R1", "R2", "R3");
        entry(table, "R2").deselect();
        assertSelection(table, "R1", "R3");
        assertEquals(/* expected */ 4L, script("return rowClicks;"));
        assertNativeClicks();
    }

    @SeleniumTestCase
    public void clicksActionAfterParentRowReplacement() {
        final DataEntryPO row = entry(table(), "R3");
        script("window.replaceOnScroll = true; showOverlay();");
        row.clickActionImage("UPDATE");
        assertEquals(/* expected */ 1L, script("return actionClicks;"));
        assertEquals(/* expected */ 0L, script("return rowClicks;"));
        assertTrue(((Number) script("return replacements;")).intValue() > 0);
        assertNativeClicks();
    }

    @SeleniumTestCase
    public void rejectsAmbiguousReplacementRows() {
        final DataEntryPO row = entry(table(), "R3");
        script("window.replaceOnScroll = true; window.duplicateOnScroll = true;");
        assertThrows(IllegalStateException.class, () -> row.clickActionImage("UPDATE"));
        assertEquals(/* expected */ 0L, script("return actionClicks;"));
    }

    @SeleniumTestCase
    public void waitsForCheckboxStateWithoutTogglingTwice() {
        final CheckBoxPO checkbox = new CheckBoxPO(getWebDriver(), getWebDriver().findElement(By.id("checkbox")));
        script("showOverlay();");
        checkbox.setSelected(true);
        assertTrue(checkbox.isSelected());
        checkbox.setSelected(true);
        assertEquals(/* expected */ 1L, script("return checkboxClicks;"));
        checkbox.setSelected(false);
        assertFalse(checkbox.isSelected());
        assertEquals(/* expected */ 2L, script("return checkboxClicks;"));
        assertNativeClicks();
    }

    @SeleniumTestCase
    public void waitsForAnimatedAndCoveredDialogButtons() {
        final DataEntryDialogPO dialog = new FixtureDialog(getWebDriver(), getWebDriver().findElement(By.id("dialog")));
        script("showOverlay(); document.getElementById('dialog').animate("
                + "[{transform:'translateX(350px)'},{transform:'translateX(0)'}], {duration:1000});");
        dialog.pressOk(/* acceptAlert */ false, /* waitForAjaxRequests */ false);
        dialog.pressCancel();
        assertEquals(/* expected */ 2L, script("return dialogClicks;"));
        assertNativeClicks();
    }

    private GenericCellTablePO<DataEntryPO> table() {
        return new GenericCellTablePO<>(getWebDriver(), getWebDriver().findElement(By.id("rows")), DataEntryPO.class);
    }

    private DataEntryPO entry(final GenericCellTablePO<DataEntryPO> table, final String name) {
        return table.getEntries().stream().filter((final DataEntryPO row) -> name.equals(row.getColumnContent("Name")))
                .findFirst().get();
    }

    private void assertSelection(final GenericCellTablePO<DataEntryPO> table, final String... names) {
        final List<String> actual = table.getSelectedEntries().stream()
                .map((final DataEntryPO row) -> row.getColumnContent("Name"))
                .sorted().collect(Collectors.toList());
        assertEquals(Arrays.asList(names), actual);
    }

    private void assertNativeClicks() {
        assertEquals(/* expected */ 0L, script("return linkClicks;"));
        assertEquals(/* expected */ 0L, script("return untrustedClicks;"));
    }

    private Object script(final String script) {
        return ((JavascriptExecutor) getWebDriver()).executeScript(script);
    }

    private static final class FixtureDialog extends DataEntryDialogPO {
        private FixtureDialog(final WebDriver driver, final WebElement element) {
            super(driver, element);
        }
    }
}
