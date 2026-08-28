package com.fxzs.lingxiagent.model.honor.dto;

import androidx.annotation.Nullable;

import java.util.List;

public class BodyData {
    @Nullable
    private String text;
    private Number cardType;
    private List<CardData> jsCards;

    public BodyData(@Nullable String text, Number cardType, List<CardData> jsCards,
                    List<ButtonsData> buttons, List<HtmlInfo> htmls) {
        this.text = text;
        this.cardType = cardType;
        this.jsCards = jsCards;
    }

    // Getters
    @Nullable
    public String getText() { return text; }
    public Number getCardType() { return cardType; }
    public List<CardData> getJsCards() { return jsCards; }

    // Setters
    public void setText(String text) { this.text = text; }
    public void setCardType(Number cardType) { this.cardType = cardType; }
    public void setJsCards(List<CardData> jsCards) { this.jsCards = jsCards; }
}
