package com.lambda256.hledger.demo;

import java.util.HashMap;

public class TxRequest {

    private String from;
    private HashMap<String, String> inputs;

    public TxRequest(String from, HashMap<String, String> inputs) {
        this.from = from;
        this.inputs = inputs;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public HashMap<String, String> getInputs() {
        return inputs;
    }

    public void setInputs(HashMap<String, String> inputs) {
        this.inputs = inputs;
    }
}
