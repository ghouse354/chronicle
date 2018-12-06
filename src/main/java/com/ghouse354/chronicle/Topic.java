package com.ghouse354.chronicle;

public abstract class Topic extends NamespaceEntity {
    public abstract String getUnit();
    public abstract String[] getAttributes();
    public abstract String getValue();
}