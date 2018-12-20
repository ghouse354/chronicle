package com.ghouse354.chronicle;

public class CValue extends CEntity {
    private String d_name;
    private String d_value;

    public CValue(String name, String value) {
        d_name = name;
        d_value = value;
    }

    public String getName() {
        return d_name;
    }

    public String getValue() {
        return d_value;
    }
}