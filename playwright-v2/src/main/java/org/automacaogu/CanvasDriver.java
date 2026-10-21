package org.automacaogu;

import com.microsoft.playwright.*;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import javax.imageio.ImageIO;

public final class CanvasDriver {
    private final Page page;
    private final Locator canvas;
    private final Path stopFile;
    private BufferedImage lastSnapshot;
    private String lastCaptureInfo;
    public CanvasDriver(Page page, Path stopFile) {
        this.page=page; this.canvas=page.locator("#gucanvas"); this.stopFile=stopFile;
    }
    public void checkStop() {
        if (Files.exists(stopFile)) throw new IllegalStateException("Execução interrompida pelo arquivo STOP.");
    }
    public BufferedImage snapshot() {
        checkStop();
        var box=canvas.boundingBox();
        if (box==null) throw new IllegalStateException("Canvas não visível. Faça login e abra o jogo.");
        Geometry.check(box.width,box.height);
        try {
            var raw=ImageIO.read(new ByteArrayInputStream(canvas.screenshot()));
            lastCaptureInfo = "Canvas CSS: " + box.width + " x " + box.height
                    + "; captura: " + raw.getWidth() + " x " + raw.getHeight()
                    + "; normalizada: " + Geometry.WIDTH + " x " + Geometry.HEIGHT;
            var normalized=new BufferedImage(Geometry.WIDTH,Geometry.HEIGHT,BufferedImage.TYPE_INT_RGB);
            var g=normalized.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(raw,0,0,Geometry.WIDTH,Geometry.HEIGHT,null);
            } finally {g.dispose();}
            lastSnapshot = normalized;
            return normalized;
        } catch(IOException e) {throw new IllegalStateException(e);}
    }
    public void await(List<Visuals.Patch> patches, Duration timeout, String state) {
        long deadline=System.nanoTime()+timeout.toNanos();
        do {
            if (Visuals.matches(snapshot(),patches)) {System.out.println("Estado confirmado: "+state); return;}
            page.waitForTimeout(250);
        } while(System.nanoTime()<deadline);
        String errors = patches.stream().map(p -> String.format(java.util.Locale.ROOT,
                "%s=%.2f%%", p.name(), 100 * Visuals.error(lastSnapshot, p)))
                .collect(java.util.stream.Collectors.joining(", "));
        throw new IllegalStateException("Tela não reconhecida: "+state+". Nenhum clique adicional será feito. Erros visuais: " + errors);
    }
    public void click(double x,double y,List<Visuals.Patch> expected) {
        if (!Visuals.matches(snapshot(),expected)) throw new IllegalStateException("Tela mudou antes do clique.");
        if (x<0 || y<0 || x>=Geometry.WIDTH || y>=Geometry.HEIGHT) throw new IllegalArgumentException("Ponto fora do canvas.");
        var b=canvas.boundingBox();
        Geometry.check(b.width,b.height);
        canvas.click(new Locator.ClickOptions().setPosition(Geometry.x(x,b.width),Geometry.y(y,b.height)));
    }
    public void save(Path output) throws IOException {ImageIO.write(snapshot(),"png",output.toFile());}
    /** Preserve the actual frame checked; do not take a later screenshot on failure. */
    public void saveLastSnapshot(Path output) throws IOException {
        if (lastSnapshot != null) {
            ImageIO.write(lastSnapshot, "png", output.toFile());
            Files.writeString(output.resolveSibling("canvas.txt"), lastCaptureInfo);
        }
    }
}
