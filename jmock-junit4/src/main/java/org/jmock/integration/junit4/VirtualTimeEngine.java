package org.jmock.integration.junit4;

import org.junit.runners.model.Statement;

public class VirtualTimeEngine {
    public void newThread(Statement statement) {
        System.out.println("Start new parallel test Statement.");
    }
}