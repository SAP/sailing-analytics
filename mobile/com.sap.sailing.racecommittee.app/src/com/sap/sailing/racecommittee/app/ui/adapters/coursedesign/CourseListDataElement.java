package com.sap.sailing.racecommittee.app.ui.adapters.coursedesign;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.PassingInstruction;

public class CourseListDataElement {
	
	private Mark leftMark;
	private Mark rightMark;
	private PassingInstruction passingInstructions;
	
	public Mark getLeftMark() {
		return leftMark;
	}
	
	public void setLeftMark(Mark mark) {
		leftMark = mark;
	}
	
	public Mark getRightMark() {
		return rightMark;
	}
	
	public void setRightMark(Mark mark) {
		rightMark = mark;
	}
	
	public PassingInstruction getPassingInstructions() {
		return passingInstructions;
	}
	
	public void setPassingInstructions(PassingInstruction instructions) {
		passingInstructions = instructions;
	}
}
