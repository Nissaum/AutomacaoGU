package org.automacaogu;

import com.microsoft.playwright.Page;
import java.time.Duration;
import java.util.Map;

/** Observes browser media events. Never seeks, changes playbackRate or fabricates completion. */
public final class VideoMonitor {
    private final Page page;
    private final Runnable cancellation;
    public VideoMonitor(Page page, Runnable cancellation) {this.page=page;this.cancellation=cancellation;}
    public void arm() {
        if (page.locator("#guVideoContainer video").count()>0
                && page.locator("#gu-video-overlay").isVisible())
            throw new IllegalStateException("Já existe um vídeo aberto. Conclua-o antes de executar.");
        page.evaluate("""
            () => {
              if (window.__guMediaCleanup) window.__guMediaCleanup();
              const s = window.__guMedia = {started:false, ended:false, error:null, time:0};
              const listen = e => {
                if (!(e.target instanceof HTMLVideoElement) ||
                    !e.target.matches('#guVideoContainer video') || !e.isTrusted) return;
                if (e.type === 'playing') s.started = true;
                if (e.type === 'timeupdate') s.time = e.target.currentTime;
                if (e.type === 'ended' && s.started && e.target.ended) s.ended = true;
                if (e.type === 'error') s.error = e.target.error?.code || 'media';
              };
              const events = ['playing','timeupdate','ended','error'];
              events.forEach(type => document.addEventListener(type, listen, true));
              window.__guMediaCleanup = () => events.forEach(type => document.removeEventListener(type, listen, true));
            }
            """);
    }
    public void awaitCompletion(Duration limit) {
        long start=System.nanoTime(),lastProgress=start;
        double lastTime=-1;
        boolean announced=false;
        while(System.nanoTime()-start < limit.toNanos()) {
            cancellation.run();
            @SuppressWarnings("unchecked")
            var state=(Map<String,Object>) page.evaluate("() => window.__guMedia");
            if (state==null) throw new IllegalStateException("Página mudou durante o vídeo.");
            if (state.get("error")!=null) throw new IllegalStateException("Player informou erro: "+state.get("error"));
            if (Boolean.TRUE.equals(state.get("ended"))) {
                System.out.println("Vídeo terminou pelo evento ended do navegador."); return;
            }
            if (Boolean.TRUE.equals(state.get("started")) && !announced) {
                System.out.println("Vídeo iniciado; aguardando reprodução normal.");announced=true;
            }
            double time=((Number)state.get("time")).doubleValue();
            if(time>lastTime+0.01) {lastTime=time;lastProgress=System.nanoTime();}
            if(!announced && System.nanoTime()-start > Duration.ofSeconds(30).toNanos())
                throw new IllegalStateException("Vídeo não iniciou em 30s. Verifique disponibilidade/autoplay.");
            if(announced && System.nanoTime()-lastProgress > Duration.ofSeconds(45).toNanos())
                throw new IllegalStateException("Vídeo sem progresso por 45s.");
            page.waitForTimeout(200);
        }
        throw new IllegalStateException("Tempo máximo do vídeo excedido; recompensa não confirmada.");
    }
}
