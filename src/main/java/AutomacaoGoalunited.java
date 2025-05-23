import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.text.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class AutomacaoGoalunited {

    private static Robot robot;
    private static boolean interrompido = false;
    private static boolean hookRegistrado = false;
    private static List<Point> coordenadasExecutadas = new ArrayList<>();

    private static final Point[] COORDENADAS_EVENTO_GU = {
            new Point(950, 845), new Point(950, 580), new Point(1290, 750)
    };

    private static final Point[] COORDENADAS_VIDEOS_GU = {
            new Point(780, 760), new Point(950, 580), new Point(1290, 750)
    };

    private static final Point[] COORDENADAS_TREINAMENTO_GU = {
            new Point(850, 700), new Point(950, 580), new Point(1200, 730)
    };

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");

        try {
            robot = new Robot();
        } catch (AWTException e) {
            JOptionPane.showMessageDialog(null, "Erro ao iniciar o robô: " + e.getMessage());
            return;
        }

        registrarTeclaEscapeGlobal();
        SwingUtilities.invokeLater(AutomacaoGoalunited::criarInterface);
    }

    private static void registrarTeclaEscapeGlobal() {
        if (hookRegistrado) return;

        try {
            if (!GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.registerNativeHook();
            }
            hookRegistrado = true;
        } catch (NativeHookException e) {
            if (e.getMessage().contains("already been registered")) {
                try {
                    GlobalScreen.unregisterNativeHook();
                    GlobalScreen.registerNativeHook();
                    hookRegistrado = true;
                } catch (NativeHookException ex) {
                    JOptionPane.showMessageDialog(null, "Erro ao reiniciar hook de teclado: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(null, "Erro ao registrar hook de teclado: " + e.getMessage());
            }
        }

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                if (e.getKeyCode() == NativeKeyEvent.VC_ESCAPE) {
                    interrompido = true;
                }
            }
            @Override public void nativeKeyReleased(NativeKeyEvent e) {}
            @Override public void nativeKeyTyped(NativeKeyEvent e) {}
        });
    }

    private static void criarInterface() {
        JFrame frame = new JFrame("Automação GoalUnited");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 550);
        frame.setLayout(new BorderLayout());

        ImageIcon imagem = new ImageIcon("A_digital_graphic_design_image_features_a_textured.png");
        if (imagem.getIconWidth() > 0) {
            JLabel imagemLabel = new JLabel(imagem);
            imagemLabel.setHorizontalAlignment(SwingConstants.CENTER);
            frame.add(imagemLabel, BorderLayout.NORTH);
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel infoLabel = new JLabel("Escolha uma opção:");
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JRadioButton eventoBtn = new JRadioButton("Evento GU");
        eventoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        eventoBtn.setSelected(true);

        JRadioButton videoBtn = new JRadioButton("Vídeos GU");
        videoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JRadioButton treinamentoBtn = new JRadioButton("Treinamento GU");
        treinamentoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(eventoBtn);
        grupo.add(videoBtn);
        grupo.add(treinamentoBtn);

        JLabel repLabel = new JLabel("Número de repetições (1-99):");
        repLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField repField = new JTextField();
        repField.setMaximumSize(new Dimension(100, 25));
        repField.setAlignmentX(Component.CENTER_ALIGNMENT);

        ((AbstractDocument) repField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                if (string.matches("\\d+")) {
                    super.insertString(fb, offset, string, attr);
                }
            }
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text.matches("\\d+")) {
                    super.replace(fb, offset, length, text, attrs);
                }
            }
        });

        JLabel escLabel = new JLabel("Pressione ESC a qualquer momento para interromper.");
        escLabel.setForeground(Color.RED);
        escLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton iniciarBtn = new JButton("Iniciar");
        iniciarBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        iniciarBtn.setMaximumSize(new Dimension(150, 30));

        iniciarBtn.addActionListener(e -> {
            try {
                int vezes = Integer.parseInt(repField.getText());
                if (vezes < 1 || vezes > 99) throw new NumberFormatException();
                Point[] coordenadas = eventoBtn.isSelected() ? COORDENADAS_EVENTO_GU :
                        (videoBtn.isSelected() ? COORDENADAS_VIDEOS_GU : COORDENADAS_TREINAMENTO_GU);
                frame.setVisible(false);
                coordenadasExecutadas.clear();
                executarAutomacao(coordenadas, vezes);
                frame.setVisible(true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Insira valores válidos.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton mapeamentoBtn = new JButton("Mapeamento de Coordenadas");
        mapeamentoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        mapeamentoBtn.setMaximumSize(new Dimension(200, 30));
        mapeamentoBtn.addActionListener(e -> main.java.CoordenadasMapper.abrirJanela());

        panel.add(infoLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(eventoBtn);
        panel.add(videoBtn);
        panel.add(treinamentoBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(repLabel);
        panel.add(repField);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(iniciarBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(mapeamentoBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(escLabel);

        frame.add(panel, BorderLayout.CENTER);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void executarAutomacao(Point[] coordenadas, int vezes) {
        interrompido = false;

        for (int i = 0; i < vezes && !interrompido; i++) {
            for (int j = 0; j < coordenadas.length && !interrompido; j++) {
                Point p = coordenadas[j];
                robot.mouseMove(p.x, p.y);
                coordenadasExecutadas.add(new Point(p));
                delay(3500);
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
                if (j == 1) {
                    pressionarTeclaX10x();
                    delay(5000);
                }
            }
        }
        if (interrompido) {
            salvarCoordenadasEmArquivo();
            JOptionPane.showMessageDialog(null, "Processo interrompido pelo usuário. Coordenadas salvas em coordenadas_log.txt");
        } else {
            JOptionPane.showMessageDialog(null, "Processo executado com sucesso!");
        }
        try {
            GlobalScreen.unregisterNativeHook();
            hookRegistrado = false;
        } catch (NativeHookException ex) {
            ex.printStackTrace();
        }
    }

    private static void salvarCoordenadasEmArquivo() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("coordenadas_log.txt", true))) {
            for (Point p : coordenadasExecutadas) {
                writer.write("X: " + p.x + " Y: " + p.y);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void pressionarTeclaX10x() {
        delay(2500);
        for (int i = 0; i < 10; i++) {
            robot.keyPress(KeyEvent.VK_X);
            delay(200);
            robot.keyRelease(KeyEvent.VK_X);
            delay(500);
        }
    }

    private static void delay(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}
