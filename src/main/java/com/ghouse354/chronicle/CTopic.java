package com.ghouse354.chronicle;

import java.util.Optional;

public abstract class CTopic extends CEntity {
    protected String d_name;
    protected String d_unit;
    protected Optional<String> d_value = Optional.of(Chronicle.DEFAULT_DATA);
    protected String[] d_attrs;

    public String[] getAttributes() {
        return d_attrs;
    }

    public String getUnit() {
        return d_unit;
    }

    public String getName() {
        return d_name;
    }

    public String getValue() {
        return d_value.get();
    }

}