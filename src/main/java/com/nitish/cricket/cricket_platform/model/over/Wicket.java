package com.nitish.cricket.cricket_platform.model.over;

public class Wicket {
    private boolean isOut;
    private String dismissalType;
    private String fielderId;

    public boolean isOut() { return isOut; }
    public void setOut(boolean out) { isOut = out; }

    public String getDismissalType() { return dismissalType; }
    public void setDismissalType(String dismissalType) { this.dismissalType = dismissalType; }

    public String getFielderId() { return fielderId; }
    public void setFielderId(String fielderId) { this.fielderId = fielderId; }
}
