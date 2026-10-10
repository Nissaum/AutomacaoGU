package org.automacaogu;

import java.time.Duration;

public final class TrainingFlow {
    private final CanvasDriver canvas;
    private final VideoMonitor video;
    private final Duration videoLimit;
    private static final Duration SCREEN_TIMEOUT=Duration.ofSeconds(20);
    public TrainingFlow(CanvasDriver canvas,VideoMonitor video,Duration videoLimit) {
        this.canvas=canvas;this.video=video;this.videoLimit=videoLimit;
    }
    /** One explicitly selected Monday slot, at most two videos. */
    public void run(String slot,boolean bonus) {
        var ready=Visuals.slot(slot,false);
        var done=Visuals.slot(slot,true);
        if (Visuals.matches(canvas.snapshot(),done)) {
            System.out.println("Slot já concluído. Nenhum vídeo solicitado.");return;
        }
        canvas.await(ready,SCREEN_TIMEOUT,"segunda-feira "+slot+":00 disponível");
        video.arm();
        canvas.click(505,slot.equals("10")?311:371,ready);
        video.awaitCompletion(videoLimit);
        collect();
        canvas.await(Visuals.BONUS,SCREEN_TIMEOUT,"oferta de segundo vídeo");
        if(bonus) {
            video.arm();
            canvas.click(471,543,Visuals.BONUS);
            video.awaitCompletion(videoLimit);
            collect();
        } else {
            canvas.click(707,111,Visuals.BONUS);
        }
        canvas.await(done,SCREEN_TIMEOUT,"slot concluído");
        System.out.println("Ciclo concluído: segunda "+slot+":00"+(bonus?", com bônus.":", sem segundo vídeo."));
    }
    private void collect() {
        canvas.await(Visuals.REWARD,SCREEN_TIMEOUT,"recompensa disponível");
        canvas.click(471,448,Visuals.REWARD);
    }
}
