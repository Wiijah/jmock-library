package org.jmock.internal.perfmodel;

import org.jmock.api.Invocation;

public interface PerformanceModel {
    void query(long threadId, Invocation invocation);
}