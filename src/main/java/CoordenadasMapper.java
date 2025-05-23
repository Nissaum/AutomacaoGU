package main.java;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.mouse.*;
import com.github.kwhat.jnativehook.NativeHookException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class CoordenadasMapper {
    public static void abrirJanela() {
        JFrame frame = new JFrame("Mapeador de Coordenadas");
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        JScrollPane scroll = new JScrollPane(area);

        JLabel instrucoes = new JLabel("Clique em qualquer lugar da tela para obter coordenadas (programa pode estar minimizado).");
        instrucoes.setHorizontalAlignment(SwingConstants.CENTER);
        frame.add(instrucoes, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.CENTER);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            JOptionPane.showMessageDialog(null, "Erro ao registrar hook global: " + e.getMessage());
            return;
        }

        GlobalScreen.addNativeMouseListener(new NativeMouseInputAdapter() {
            @Override
            public void nativeMousePressed(NativeMouseEvent e) {
                SwingUtilities.invokeLater(() -> area.append("X: " + e.getX() + " Y: " + e.getY() + "\n"));
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                try {
                    GlobalScreen.unregisterNativeHook();
                } catch (NativeHookException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}