package org.automacaogu;

/** Coordinates are measured inside the canvas, never on the desktop. */
public final class Geometry {
    public static final int WIDTH = 942, HEIGHT = 671;
    private Geometry() {}
    public static void check(double width, double height) {
        if (width < 470 || height < 335 || Math.abs(width / height - (double) WIDTH / HEIGHT) > 0.014)
            throw new IllegalStateException("Canvas incompatível: " + width + " x " + height + ". Recalibre antes de clicar.");
    }
    public static double x(double x, double width) { return x * width / WIDTH; }
    public static double y(double y, double height) { return y * height / HEIGHT; }
}
