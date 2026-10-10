package org.automacaogu;

import com.microsoft.playwright.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class Main {
    public static void main(String[] args) throws Exception {
        var options=parse(args);
        if(options.containsKey("help")) {help();return;}
        String flow=options.getOrDefault("flow","diagnose");
        if(!Set.of("diagnose","map","training").contains(flow)) throw new IllegalArgumentException("Fluxo inválido: "+flow);
        String slot=options.getOrDefault("slot","10");
        Visuals.slot(slot,false); // validate before browser is launched
        long videoSeconds=Long.parseLong(options.getOrDefault("video-timeout","180"));
        if(videoSeconds<1 || videoSeconds>600) throw new IllegalArgumentException("video-timeout deve ser 1..600 segundos.");
        Path stop=Path.of("STOP").toAbsolutePath();
        if(Files.exists(stop)) throw new IllegalStateException("Remova o arquivo STOP para iniciar uma nova execução.");
        Path run=Path.of("runs",DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS").format(LocalDateTime.now()));
        Files.createDirectories(run);
        try(var playwright=Playwright.create()) {
            var launch=new BrowserType.LaunchPersistentContextOptions().setHeadless(false).setViewportSize(1280,671);
            if(options.containsKey("channel")) launch.setChannel(options.get("channel"));
            try(var context=playwright.chromium().launchPersistentContext(Path.of(".gu-profile"),launch)) {
                context.setDefaultTimeout(10_000);
                var page=context.pages().isEmpty()?context.newPage():context.pages().get(0);
                page.navigate(options.getOrDefault("url","https://game.goalunited.org/html5/index.php"));
                var canvas=new CanvasDriver(page,stop);
                try {
                    System.out.println("Faça login manualmente. Para training, abra Time > Treinamento e deixe a tela sem diálogos.");
                    System.out.println("Pressione ENTER aqui quando estiver pronto. Para interromper: Ctrl+C ou arquivo STOP.");
                    var input=new BufferedReader(new InputStreamReader(System.in));
                    if(input.readLine()==null) throw new IllegalStateException("Console sem entrada interativa.");
                    canvas.save(run.resolve("before.png"));
                    switch(flow) {
                        case "training" -> new TrainingFlow(canvas,new VideoMonitor(page,canvas::checkStop),Duration.ofSeconds(videoSeconds))
                                .run(slot,options.containsKey("bonus"));
                        case "map" -> {
                            page.onConsoleMessage(msg -> {
                                if(msg.text().startsWith("GU_POINT ")) {
                                    System.out.println(msg.text());
                                    try {Files.writeString(run.resolve("points.log"),msg.text()+System.lineSeparator(),
                                            StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
                                    catch(IOException e) {throw new UncheckedIOException(e);}
                                }
                            });
                            page.evaluate("""
                                () => document.querySelector('#gucanvas').addEventListener('pointerdown', e => {
                                  const b=e.currentTarget.getBoundingClientRect();
                                  console.log('GU_POINT '+Math.round((e.clientX-b.left)*942/b.width)+','+
                                    Math.round((e.clientY-b.top)*671/b.height));
                                }, true)
                                """);
                            System.out.println("Clique nos controles para imprimir pontos relativos ao canvas. ENTER encerra o mapeamento.");
                            // Keep Playwright dispatching browser events while waiting for console input.
                            var finished=new java.util.concurrent.atomic.AtomicBoolean();
                            var reader=new Thread(() -> {try {input.readLine();} catch(IOException ignored) {} finally {finished.set(true);}},"console-reader");
                            reader.setDaemon(true);reader.start();
                            while(!finished.get()) {canvas.checkStop();page.waitForTimeout(100);}
                        }
                        default -> System.out.println("Diagnóstico salvo. Canvas compatível com a proporção 942 x 671.");
                    }
                    canvas.save(run.resolve("after.png"));
                    System.out.println("Evidências: "+run.toAbsolutePath());
                } catch(Exception e) {
                    try {page.screenshot(new Page.ScreenshotOptions().setPath(run.resolve("error.png")));} catch(Exception ignored) {}
                    Files.writeString(run.resolve("error.txt"),e.toString());
                    throw e;
                }
            }
        }
    }
    static Map<String,String> parse(String[] args) {
        var result=new HashMap<String,String>();
        Set<String> flags=Set.of("bonus","help"),values=Set.of("flow","slot","url","channel","video-timeout");
        for(int i=0;i<args.length;i++) {
            if(!args[i].startsWith("--")) throw new IllegalArgumentException("Argumento inválido: "+args[i]);
            String key=args[i].substring(2);
            if(result.containsKey(key)) throw new IllegalArgumentException("Argumento repetido: "+key);
            if(flags.contains(key)) result.put(key,"true");
            else if(values.contains(key) && i+1<args.length && !args[i+1].startsWith("--")) result.put(key,args[++i]);
            else throw new IllegalArgumentException("Opção desconhecida ou sem valor: "+key);
        }
        return result;
    }
    private static void help() {
        System.out.println("""
            Java + Playwright GoalUnited (Java 17)
            --flow diagnose|map|training   (padrão: diagnose, sem cliques automáticos)
            --slot 10|12                  segunda-feira, um slot por execução
            --bonus                       assistir também o segundo vídeo
            --video-timeout 180            máximo por vídeo em segundos, 1..600
            --channel chrome              opcional: Chrome instalado em vez de Chromium
            --url URL                     padrão: https://game.goalunited.org/html5/index.php
            Login manual no perfil exclusivo .gu-profile; nunca use seu perfil Chrome habitual.
            """);
    }
}
