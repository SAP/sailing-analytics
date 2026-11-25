package com.sap.sailing.selenium.pages.adminconsole.regatta;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.core.BySeleniumId;
import com.sap.sailing.selenium.core.FindBy;
import com.sap.sailing.selenium.pages.PageArea;
import com.sap.sailing.selenium.pages.adminconsole.ActionsHelper;
import com.sap.sailing.selenium.pages.gwt.CellTablePO;
import com.sap.sailing.selenium.pages.gwt.CellTablePO.SortingOrder;
import com.sap.sailing.selenium.pages.gwt.DataEntryPO;
import com.sap.sailing.selenium.pages.gwt.GenericCellTablePO;

public class RegattaListCompositePO extends PageArea {
    public static class RegattaDescriptor {
        private final String name;
        private final String boatClass;
        private final CompetitorRegistrationType competitorRegistrationType;
        private final String registrationLinkSecret;
        
        // TODO [D049941]: Mark as a bug and remove this method as soon as possible
        public static RegattaDescriptor fromString(String name) {
            if(name == null || name.isEmpty())
                return null;
            
            int openingBrace = name.lastIndexOf('(');
            int closingBrace = name.lastIndexOf(')');
            
            return new RegattaDescriptor(name.substring(0, openingBrace - 1),
                    name.substring(openingBrace + 1, closingBrace));
        }

        public RegattaDescriptor(String name, String boatClass) {
            this(name, boatClass, CompetitorRegistrationType.CLOSED, null);
        }

        public RegattaDescriptor(String name, String boatClass, CompetitorRegistrationType competitorRegistrationType,
                String registrationLinkSecret) {
            this.name = name;
            this.boatClass = boatClass;
            this.competitorRegistrationType = competitorRegistrationType;
            this.registrationLinkSecret = registrationLinkSecret;
        }

        public String getName() {
            return this.name;
        }

        public String getBoatClass() {
            return this.boatClass;
        }

        public CompetitorRegistrationType getCompetitorRegistrationType() {
            return competitorRegistrationType;
        }

        public String getRegistrationLinkSecret() {
            return registrationLinkSecret;
        }

        public String toString() {
            return this.name + " (" + this.boatClass + ")";
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.name, this.boatClass);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object)
                return true;
            
            if (object == null)
                return false;
            
            if (getClass() != object.getClass())
                return false;
            
            RegattaDescriptor other = (RegattaDescriptor) object;
            
            if (!Objects.equals(this.boatClass, other.boatClass))
                return false;
            
            if (!Objects.equals(this.name, other.name))
                return false;
            
            return true;
        }
    }
    
    @FindBy(how = BySeleniumId.class, using = "FilterRegattasTextField")
    private WebElement RegattasFilterTextField;
    
    @FindBy(how = BySeleniumId.class, using = "NoRegattasLabel")
    private WebElement noRegattasLabel;
    
    @FindBy(how = BySeleniumId.class, using = "RegattasCellTable")
    private WebElement regattasTable;
    
    public RegattaListCompositePO(WebDriver driver, WebElement element) {
        super(driver, element);
    }
    
    public void sortByRegattaName(SortingOrder order) {
        sortRegattaList(0, order);
    }
    
    public void sortByBoatClass(SortingOrder order) {
        sortRegattaList(1, order);
    }
    
    public List<RegattaDescriptor> getRegattas() {
        List<RegattaDescriptor> descriptors = new LinkedList<>();
        CellTablePO<DataEntryPO> table = getRegattasTable();
        
        for(DataEntryPO entry : table.getEntries()) {
            String name = entry.getColumnContent("Regatta name");
            String boatClass = entry.getColumnContent("Boat Class");
            
            descriptors.add(new RegattaDescriptor(name, boatClass));
        }
        
        return descriptors;
    }
    
    public void selectRegatta(RegattaDescriptor regatta) {
        DataEntryPO entry = findRegatta(regatta);
        
        if(entry != null) {
            entry.select();
        }
    }
    
//    public EditRegattaDialog editRegatta(RegattaDescriptor regatta) {
//        
//    }
    
    public void removeRegatta(RegattaDescriptor regatta) {
        DataEntryPO entry = findRegatta(regatta);
        if (entry != null) {
            entry.clickActionImage(ActionsHelper.DELETE_ACTION);
            waitForAjaxRequests();
            ActionsHelper.acceptAlert(this.driver);
        }
    }
    
    public RegattaEditDialogPO editRegatta(RegattaDescriptor regatta) {
        DataEntryPO entry = findRegatta(regatta);
        if (entry != null) {
            entry.clickActionImage(ActionsHelper.UPDATE_ACTION);
            WebElement dialog = findElementBySeleniumId(this.driver, "RegattaWithSeriesAndFleetsEditDialog");
            return new RegattaEditDialogPO(this.driver, dialog);
        }
        return null;
    }
    
    private DataEntryPO findRegatta(RegattaDescriptor regatta) {
        CellTablePO<DataEntryPO> table = getRegattasTable();
        
        for(DataEntryPO entry : table.getEntries()) {
            String name = entry.getColumnContent("Regatta name");
//            String boatClass = columns.getColumnContent(1);
//            
//            if(regatta.equals(new RegattaDescriptor(name, boatClass)))
//                return row;
            if(regatta.equals(RegattaDescriptor.fromString(name)))
                return entry;
        }
        
        return null;
    }
    
    private void sortRegattaList(int column, SortingOrder order) {
        CellTablePO<DataEntryPO> table = getRegattasTable();
        table.sortByColumn(column, order);
    }
    
    private CellTablePO<DataEntryPO> getRegattasTable() {
        return new GenericCellTablePO<>(this.driver, this.regattasTable, DataEntryPO.class);
    }
}
