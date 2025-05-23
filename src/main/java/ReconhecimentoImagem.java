import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class ReconhecimentoImagem {

    static {
        // Carrega a biblioteca nativa do OpenCV
        nu.pattern.OpenCV.loadShared();
    }

    public static void adicionarBotao(JFrame frame, JPanel panel) {
        JButton reconhecimentoBtn = new JButton("Reconhecimento de Imagem");
        reconhecimentoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        reconhecimentoBtn.setMaximumSize(new Dimension(200, 30));
        reconhecimentoBtn.addActionListener(e -> executar());

        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(reconhecimentoBtn);
    }

    public static void executar() {
        try {
            // Solicita imagem alvo ao usuário
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Selecione a imagem a ser localizada na tela");
            int result = chooser.showOpenDialog(null);
            if (result != JFileChooser.APPROVE_OPTION) {
                JOptionPane.showMessageDialog(null, "Operação cancelada.");
                return;
            }

            File imagemArquivo = chooser.getSelectedFile();
            Mat imagemModelo = Imgcodecs.imread(imagemArquivo.getAbsolutePath());
            if (imagemModelo.empty()) {
                JOptionPane.showMessageDialog(null, "Imagem inválida ou não pôde ser carregada.");
                return;
            }

            // Captura a tela inteira
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            Robot robot = new Robot();
            BufferedImage screenshot = robot.createScreenCapture(screenRect);

            // Converte screenshot para Mat (formato OpenCV)
            Mat tela = bufferedImageToMat(screenshot);

            // Faz a correspondência do template
            Mat resultado = new Mat();
            Imgproc.matchTemplate(tela, imagemModelo, resultado, Imgproc.TM_CCOEFF_NORMED);

            Core.MinMaxLocResult mmr = Core.minMaxLoc(resultado);
            if (mmr.maxVal >= 0.8) { // limiar de confiança
                Point matchLoc = mmr.maxLoc;
                int x = (int) matchLoc.x + imagemModelo.width() / 2;
                int y = (int) matchLoc.y + imagemModelo.height() / 2;
                robot.mouseMove(x, y);
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                JOptionPane.showMessageDialog(null, "Imagem encontrada e clicada em: (" + x + ", " + y + ")");
            } else {
                JOptionPane.showMessageDialog(null, "Imagem não encontrada na tela.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Erro ao processar imagem: " + e.getMessage());
        }
    }

    private static Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        int[] data = new int[bi.getWidth() * bi.getHeight()];
        bi.getRGB(0, 0, bi.getWidth(), bi.getHeight(), data, 0, bi.getWidth());
        byte[] bytes = new byte[bi.getWidth() * bi.getHeight() * 3];
        for (int i = 0; i < data.length; i++) {
            bytes[i * 3] = (byte) ((data[i] >> 16) & 0xFF); // R
            bytes[i * 3 + 1] = (byte) ((data[i] >> 8) & 0xFF);  // G
            bytes[i * 3 + 2] = (byte) (data[i] & 0xFF);         // B
        }
        mat.put(0, 0, bytes);
        return mat;
    }
}
