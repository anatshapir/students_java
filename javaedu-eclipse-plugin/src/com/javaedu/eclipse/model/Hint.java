package com.javaedu.eclipse.model;

/**
 * Model representing a hint for an exercise.
 */
public class Hint {
    private Long id;
    private int orderNum;
    private String content;
    private int penaltyPercentage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(int orderNum) {
        this.orderNum = orderNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getPenaltyPercentage() {
        return penaltyPercentage;
    }

    public void setPenaltyPercentage(int penaltyPercentage) {
        this.penaltyPercentage = penaltyPercentage;
    }
}
