package com.sap.sailing.gwt.ui.shared;

import java.util.List;

import com.sap.sailing.domain.common.dto.NamedDTO;

@SuppressWarnings("serial")
public class SidelineDTO extends NamedDTO {
    private List<MarkDTO> marks;

    SidelineDTO() {}

    public SidelineDTO(String name, List<MarkDTO> marks) {
        super(name);
        this.marks = marks;
    }

    public List<MarkDTO> getMarks() {
        return marks;
    }
}
