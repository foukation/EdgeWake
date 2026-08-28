package com.fxzs.lingxiagent.view.drawing;

public class DrawingStyleItem {
    private String name;
    private int imageResource;
    private boolean isSelected;

    public DrawingStyleItem(String name, int imageResource, boolean isSelected) {
        this.name = name;
        this.imageResource = imageResource;
        this.isSelected = isSelected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImageResource() {
        return imageResource;
    }

    public void setImageResource(int imageResource) {
        this.imageResource = imageResource;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}

