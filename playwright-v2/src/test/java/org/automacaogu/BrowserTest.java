package org.automacaogu;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/** Local fixture only: real media playback, no GoalUnited account/network requests. */
class BrowserTest {
    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;
    CanvasDriver canvas;
    VideoMonitor video;
    @TempDir Path temp;
    @BeforeAll static void browser() {
        playwright=Playwright.create();
        browser=playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }
    @AfterAll static void closeBrowser() {if(browser!=null)browser.close();if(playwright!=null)playwright.close();}
    @BeforeEach void fixture() throws Exception {
        context=browser.newContext(new Browser.NewContextOptions().setViewportSize(942,671));page=context.newPage();
        page.setContent("<style>body{margin:0} #gu-video-overlay{position:absolute;inset:0;display:none;background:black}</style>"
            +"<canvas id='gucanvas' width='942' height='671'></canvas>"
            +"<div id='gu-video-overlay'><div id='guVideoContainer'></div></div>");
        canvas=new CanvasDriver(page,temp.resolve("STOP"));video=new VideoMonitor(page,canvas::checkStop);
        var patches=new LinkedHashMap<String,Object>();
        var all=new ArrayList<Visuals.Patch>();
        all.addAll(Visuals.slot("10",false));all.addAll(Visuals.slot("10",true));
        all.addAll(Visuals.slot("12",false));all.addAll(Visuals.slot("12",true));
        all.addAll(Visuals.REWARD);all.addAll(Visuals.BONUS);
        for(var p:all) {
            try(var in=getClass().getResourceAsStream("/templates/"+p.name()+".png")) {
                patches.put(p.name(),Map.of("x",p.x(),"y",p.y(),"url","data:image/png;base64,"+Base64.getEncoder().encodeToString(in.readAllBytes())));
            }
        }
        page.evaluate("""
            async patches => {
              window.images = {};
              for (const [name,p] of Object.entries(patches)) {
                const image=new Image(); image.src=p.url; await image.decode(); images[name]={...p,image};
              }
              window.draw = names => {
                const c=document.querySelector('canvas').getContext('2d');c.fillStyle='#24440e';c.fillRect(0,0,942,671);
                names.forEach(name => {const p=images[name];c.drawImage(p.image,p.x,p.y);});
              };
              window.plays=0; window.stage='ready'; window.selectedSlot='10'; window.wantBonus=true;
              window.training = done => draw(['training','slot-'+selectedSlot+(done?'-done':'-ready')]);
              window.reward = () => {stage='reward';draw(['reward-label','collect']);};
              window.startVideo = () => {
                plays++;stage='video'; document.querySelector('#gu-video-overlay').style.display='block';
                const v=document.createElement('video');v.muted=true;v.src=window.clip;
                document.querySelector('#guVideoContainer').appendChild(v);
                v.addEventListener('ended',()=>{v.remove();document.querySelector('#gu-video-overlay').style.display='none';reward();});
                v.play();
              };
              document.querySelector('canvas').addEventListener('click',e => {
                if(stage==='ready' && Math.abs(e.offsetX-505)<10 && Math.abs(e.offsetY-(selectedSlot==='10'?311:371))<10) startVideo();
                else if(stage==='reward' && Math.abs(e.offsetX-471)<10 && Math.abs(e.offsetY-448)<10) {
                  if(plays===1){stage='bonus';draw(['bonus-label','bonus-button']);}
                  else {stage='done';training(true);}
                } else if(stage==='bonus') {
                  if(Math.abs(e.offsetX-471)<10 && Math.abs(e.offsetY-543)<10) startVideo();
                  else if(Math.abs(e.offsetX-707)<10 && Math.abs(e.offsetY-111)<10){stage='done';training(true);}
                }
              });
              training(false);
            }
            """,patches);
        try(var in=getClass().getResourceAsStream("/clip.webm")) {
            page.evaluate("clip => window.clip=clip","data:video/webm;base64,"+Base64.getEncoder().encodeToString(in.readAllBytes()));
        }
    }
    @AfterEach void closeContext() {if(context!=null)context.close();}
    @Test void completesOneTrainingSlotWithTwoRealVideos() {
        new TrainingFlow(canvas,video,Duration.ofSeconds(10)).run("10",true);
        assertEquals(2,((Number)page.evaluate("plays")).intValue());
        assertEquals("done",page.evaluate("stage"));
    }
    @Test void secondSlotWithoutBonusPlaysOnlyOneVideo() {
        page.evaluate("() => {selectedSlot='12';training(false);}");
        new TrainingFlow(canvas,video,Duration.ofSeconds(10)).run("12",false);
        assertEquals(1,((Number)page.evaluate("plays")).intValue());
        assertEquals("done",page.evaluate("stage"));
    }
    @Test void completedSlotDoesNotPlayAgain() {
        page.evaluate("() => {stage='done';training(true);}");
        new TrainingFlow(canvas,video,Duration.ofSeconds(10)).run("10",true);
        assertEquals(0,((Number)page.evaluate("plays")).intValue());
    }
    @Test void syntheticEndedIsNotAccepted() {
        video.arm();
        page.evaluate("() => {const v=document.createElement('video');document.querySelector('#guVideoContainer').appendChild(v);v.dispatchEvent(new Event('playing'));v.dispatchEvent(new Event('ended'));}");
        assertThrows(IllegalStateException.class,()->video.awaitCompletion(Duration.ofMillis(600)));
    }
    @Test void disappearingPlayerIsNotCompletion() {
        video.arm();page.evaluate("() => {startVideo();setTimeout(()=>document.querySelector('video').remove(),100);}");
        assertThrows(IllegalStateException.class,()->video.awaitCompletion(Duration.ofSeconds(2)));
    }
    @Test void unexpectedScreenPreventsClicks() {
        page.evaluate("() => draw(['bonus-label','bonus-button'])");
        assertThrows(IllegalStateException.class,()->canvas.click(505,311,Visuals.slot("10",false)));
        assertEquals(0,((Number)page.evaluate("plays")).intValue());
    }
    @Test void stopFileCancelsBeforeClick() throws Exception {
        Files.createFile(temp.resolve("STOP"));
        assertThrows(IllegalStateException.class,()->canvas.click(505,311,Visuals.slot("10",false)));
        assertEquals(0,((Number)page.evaluate("plays")).intValue());
    }
}
