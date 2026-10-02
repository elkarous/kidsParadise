package com.kindererp.service.dto;

import com.kindererp.model.Parent;

/** A parent with what the parents screen shows next to it. */
public record ParentSummary(Parent parent, FeeBreakdown fee) {

    public int childCount() {
        return fee.childCount();
    }
}
