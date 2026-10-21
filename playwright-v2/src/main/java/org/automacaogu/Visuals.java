package org.automacaogu;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

public final class Visuals {
    public record Patch(String name, int x, int y, BufferedImage image, List<BufferedImage> alternatives) {}
    public static final double MAX_ERROR = 0.025;
    private Visuals() {}
    public static Patch patch(String name, int x, int y) {
        try (var in = Visuals.class.getResourceAsStream("/templates/" + name + ".png")) {
            if (in == null) throw new IllegalArgumentException("Template ausente: " + name);
            var image = ImageIO.read(in);
            try (var alternative = Visuals.class.getResourceAsStream("/templates/" + name + "-chromium.png")) {
                return new Patch(name, x, y, image,
                        alternative == null ? List.of() : List.of(ImageIO.read(alternative)));
            }
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    /** Mean absolute RGB distance at the calibrated location. No arbitrary search/click. */
    public static double error(BufferedImage screen, Patch patch) {
        double best = pixelError(screen, patch.x(), patch.y(), patch.image());
        for (var alternative : patch.alternatives())
            best = Math.min(best, pixelError(screen, patch.x(), patch.y(), alternative));
        return best;
    }
    private static double pixelError(BufferedImage screen, int left, int top, BufferedImage t) {
        if (left < 0 || top < 0 || left+t.getWidth() > screen.getWidth()
                || top+t.getHeight() > screen.getHeight()) return 1;
        long sum = 0;
        for (int y=0; y<t.getHeight(); y++) for (int x=0; x<t.getWidth(); x++) {
            int a=screen.getRGB(left+x,top+y), b=t.getRGB(x,y);
            for (int shift : new int[]{0,8,16}) sum+=Math.abs(((a>>shift)&255)-((b>>shift)&255));
        }
        return sum / (255.0 * 3 * t.getWidth() * t.getHeight());
    }
    public static boolean matches(BufferedImage screen, List<Patch> patches) {
        return patches.stream().allMatch(p -> error(screen,p) <= MAX_ERROR);
    }
    public static String trainingReport(BufferedImage screen) {
        var report = new StringBuilder("Canvas normalizado: 942 x 671. Limite de erro visual: 2,50%.\n");
        for (String slot : List.of("10", "12")) {
            var ready = slot(slot, false);
            var done = slot(slot, true);
            String state = matches(screen, ready) ? "DISPONÍVEL" : matches(screen, done) ? "CONCLUÍDO" : "NÃO RECONHECIDO";
            report.append("Segunda ").append(slot).append(":00: ").append(state).append('\n');
            for (var p : List.of(TRAINING, ready.get(1), done.get(1)))
                report.append(String.format(Locale.ROOT, "  %s: erro %.2f%%%n", p.name(), 100 * error(screen, p)));
        }
        return report.toString();
    }
    public static final Patch TRAINING=patch("training",54,99);
    public static final List<Patch> REWARD=List.of(patch("reward-label",302,277),patch("collect",388,433));
    public static final List<Patch> BONUS=List.of(patch("bonus-label",255,191),patch("bonus-button",411,528));
    public static List<Patch> slot(String slot, boolean done) {
        int y=switch(slot) {case "10" -> 302; case "12" -> 362; default -> throw new IllegalArgumentException("Slot deve ser 10 ou 12.");};
        return List.of(TRAINING,patch("slot-"+slot+(done?"-done":"-ready"),496,y));
    }
}
