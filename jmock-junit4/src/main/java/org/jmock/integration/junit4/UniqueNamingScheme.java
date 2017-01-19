package org.jmock.integration.junit4;

import org.jmock.lib.CamelCaseNamingScheme;

public class UniqueNamingScheme extends CamelCaseNamingScheme {
    public static final UniqueNamingScheme INSTANCE = new UniqueNamingScheme();

    @Override
    public String defaultNameFor(Class<?> typeToMock) {
        String name = super.defaultNameFor(typeToMock);
        return name + "-" + Thread.currentThread().getId();
    }
}