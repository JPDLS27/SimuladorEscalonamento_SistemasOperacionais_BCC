package simulador.gui;

import javax.swing.*;

/** Ponto de entrada da interface gráfica. */
public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels())
                    if ("Nimbus".equals(i.getName())) UIManager.setLookAndFeel(i.getClassName());
            } catch (Exception ignorado) { }
            new TelaPrincipal().setVisible(true);
        });
    }
}
