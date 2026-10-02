package com.kindererp.util;

import javafx.scene.Node;
import javafx.scene.shape.SVGPath;

/** Small vector icons (no icon-font dependency). Colour comes from the {@code icon} CSS class. */
public final class Icons {

    private static final String TRASH = "M3 6h18v2H3zM8 6V4h8v2h-2V5h-4v1zM5 9h14l-1.2 12H6.2zM9 11v8h2v-8zm4 0v8h2v-8z";
    private static final String PRINT = "M6 2h12v5H6zM4 8h16a2 2 0 0 1 2 2v6h-4v6H6v-6H2v-6a2 2 0 0 1 2-2zm4 9v3h8v-3zm10-6a1 1 0 1 0 0 2 1 1 0 0 0 0-2z";

    private Icons() {
    }

    public static Node trash() {
        return icon(TRASH);
    }

    public static Node print() {
        return icon(PRINT);
    }

    private static Node icon(String path) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add("icon");
        svg.setScaleX(0.75);
        svg.setScaleY(0.75);
        return svg;
    }
}
