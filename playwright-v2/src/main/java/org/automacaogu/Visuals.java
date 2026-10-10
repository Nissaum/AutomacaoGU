package org.automacaogu;

import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;

public final class Visuals {
    public record Patch(String name, int x, int y, BufferedImage image) {}
    private Visuals() {}
    public static Patch patch(String name, int x, int y) {
        try (var in = Visuals.class.getResourceAsStream("/templates/" + name + ".png")) {
            if (in == null) throw new IllegalArgumentException("Template ausente: " + name);
            return new Patch(name, x, y, ImageIO.read(in));
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    /** Mean absolute RGB distance at the calibrated location. No arbitrary search/click. */
    public static double error(BufferedImage screen, Patch patch) {
        var t = patch.image();
        if (patch.x() < 0 || patch.y() < 0 || patch.x()+t.getWidth() > screen.getWidth()
                || patch.y()+t.getHeight() > screen.getHeight()) return 1;
        long sum = 0;
        for (int y=0; y<t.getHeight(); y++) for (int x=0; x<t.getWidth(); x++) {
            int a=screen.getRGB(patch.x()+x,patch.y()+y), b=t.getRGB(x,y);
            for (int shift : new int[]{0,8,16}) sum+=Math.abs(((a>>shift)&255)-((b>>shift)&255));
        }
        return sum / (255.0 * 3 * t.getWidth() * t.getHeight());
    }
    public static boolean matches(BufferedImage screen, List<Patch> patches) {
        return patches.stream().allMatch(p -> error(screen,p) <= 0.025);
    }
    public static final Patch TRAINING=patch("training",54,99);
    public static final List<Patch> REWARD=List.of(patch("reward-label",302,277),patch("collect",388,433));
    public static final List<Patch> BONUS=List.of(patch("bonus-label",255,191),patch("bonus-button",411,528));
    public static List<Patch> slot(String slot, boolean done) {
        int y=switch(slot) {case "10" -> 302; case "12" -> 362; default -> throw new IllegalArgumentException("Slot deve ser 10 ou 12.");};
        return List.of(TRAINING,patch("slot-"+slot+(done?"-done":"-ready"),496,y));
    }
}
