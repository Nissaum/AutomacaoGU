package org.automacaogu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

class CoreTest {
    @Test void coordinatesFollowCanvasScaleAndRejectDistortion() {
        Geometry.check(1884,1342);
        assertEquals(1010,Geometry.x(505,1884));
        assertEquals(622,Geometry.y(311,1342));
        assertThrows(IllegalStateException.class,()->Geometry.check(942,800));
        assertThrows(IllegalStateException.class,()->Geometry.check(0,0));
    }
    @Test void availableAndCompletedSlotsAreDifferent() {
        for(String slot : new String[]{"10","12"}) {
            var ready=Visuals.slot(slot,false);
            var done=Visuals.slot(slot,true);
            var screen=new BufferedImage(942,671,BufferedImage.TYPE_INT_RGB);
            var g=screen.createGraphics();
            ready.forEach(p->g.drawImage(p.image(),p.x(),p.y(),null));g.dispose();
            assertTrue(Visuals.matches(screen,ready));
            assertFalse(Visuals.matches(screen,done),"Completed slot must not match available: "+slot);
            assertFalse(Visuals.matches(screen,Visuals.REWARD));
            assertFalse(Visuals.matches(screen,Visuals.BONUS));
        }
    }
    @Test void wrongCanvasCannotMatchReward() {
        assertFalse(Visuals.matches(new BufferedImage(942,671,BufferedImage.TYPE_INT_RGB),Visuals.REWARD));
    }
    @Test void recognizesRealChromiumCaptureWithoutConfusingDialogsOrCompletion() throws Exception {
        try (var in = getClass().getResourceAsStream("/training-windows.png")) {
            var screen = ImageIO.read(in);
            for (String slot : new String[]{"10", "12"}) {
                assertTrue(Visuals.matches(screen, Visuals.slot(slot, false)), slot);
                assertFalse(Visuals.matches(screen, Visuals.slot(slot, true)), slot);
            }
            assertFalse(Visuals.matches(screen, Visuals.REWARD));
            assertFalse(Visuals.matches(screen, Visuals.BONUS));
            assertTrue(Visuals.trainingReport(screen).contains("Segunda 10:00: DISPONÍVEL"));
            assertTrue(Visuals.trainingReport(screen).contains("Segunda 12:00: DISPONÍVEL"));
        }
    }
    @Test void cliRejectsTyposMissingValuesAndDuplicates() {
        assertEquals("12",Main.parse(new String[]{"--flow","training","--slot","12","--bonus"}).get("slot"));
        assertThrows(IllegalArgumentException.class,()->Main.parse(new String[]{"--slots","12"}));
        assertThrows(IllegalArgumentException.class,()->Main.parse(new String[]{"--slot"}));
        assertThrows(IllegalArgumentException.class,()->Main.parse(new String[]{"--bonus","--bonus"}));
    }
}
