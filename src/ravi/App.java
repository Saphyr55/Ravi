/*

Projet TLA 2023-24

Réalisé par :
- NOM Prénom 
- NOM Prénom
- NOM Prénom
- NOM Prénom
- NOM Prénom

*/

package ravi;

import ravi.resolver.Interpreter;
import ravi.resolver.ScopeResolver;
import ravi.syntax.Lexer;
import ravi.syntax.Parser;
import ravi.syntax.model.Program;
import ravi.syntax.Token;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/*
 * Classe principale.
 * 
 * Gère l'IHM.
 * Affiche les lieux et propositions suivant les décisions du joueur.
 */

public class App implements ActionListener {

    public static void main(String[] args) throws IOException {

        String source = Files.readString(Path.of("game2.ravi"), StandardCharsets.UTF_8);
        Lexer lexer = new Lexer(source, LinkedList::new);
        List<Token> tokens = lexer.scan();
        // System.out.println(Arrays.toString(tokens.toArray()));

        Parser parser = new Parser(tokens);
        Program program = parser.program();
        // System.out.println(program);

        Interpreter interpreter = new Interpreter();
        ScopeResolver scopeResolver = new ScopeResolver(interpreter);

        scopeResolver.resolve(program);
        interpreter.interpretProgram(program);

        SwingUtilities.invokeLater(new App()::init);
    }

    // Nombre de lignes dans la zone de texte
    final int nbLignes = 20;

    Map<Integer, Lieu> lieux;
    Lieu lieuActuel;

    JFrame frame;
    JPanel mainPanel;

    // Labels composant la zone de texte
    JLabel[] labels;

    // Boutons de proposition
    ArrayList<JButton> btns;

    public void init() {

        // Charge le contenu de l'aventure
        lieux = ContenuAventure.init();

        // Prépare l'IHM
        labels = new JLabel[nbLignes];
        btns = new ArrayList<>();

        frame = new JFrame(ContenuAventure.titre);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        frame.add(mainPanel);

        for(int i=0;i<nbLignes;i++) {
            labels[i] = new JLabel(" ");
            mainPanel.add(labels[i], new GridBagConstraints() {{
                this.gridwidth = GridBagConstraints.REMAINDER;
                this.anchor = GridBagConstraints.WEST;
                this.insets = new Insets(0,20,0,20);
            }});
            labels[i].setMinimumSize(new Dimension(750, 20));
            labels[i].setPreferredSize(new Dimension(750, 20));
        }

        // Démarre l'aventure au lieu n° 1
        lieuActuel = lieux.get(1);
        initLieu();

        frame.pack();
        frame.setVisible(true);
    }

    /*
     * Affichage du lieu lieuActuel et créations des boutons de propositions correspondantes
     * à ce lieu
     */
    void initLieu() {
        for(JButton btn: btns) {
            mainPanel.remove(btn);
        }
        btns.clear();
        affiche(lieuActuel.description.split("\n"));
        frame.pack();
        for(int i=0; i<lieuActuel.propositions.size(); i++) {
            JButton btn = new JButton("<html><p>" + lieuActuel.propositions.get(i).texte + "</p></html>");
            btn.setActionCommand(String.valueOf(i));
            btn.addActionListener(this);
            mainPanel.add(btn, new GridBagConstraints() {{
                this.gridwidth = GridBagConstraints.REMAINDER;
                this.fill = GridBagConstraints.HORIZONTAL;
                this.insets = new Insets(3,20,3,20);
            }});
            btns.add(btn);
        }
        frame.pack();
    }

    /*
     * Gère les clics sur les boutons de propostion
     */
    public void actionPerformed(ActionEvent event) {

        // Retrouve l'index de la proposition
        int index = Integer.valueOf(event.getActionCommand());

        // Retrouve la propostion
        Proposition proposition = lieuActuel.propositions.get(index);

        // Recherche le lieu désigné par la proposition
        Lieu lieu = lieux.get(proposition.numeroLieu);
        if (lieu != null) {

            // Affiche la proposition qui vient d'être choisie par le joueur
            affiche(new String[]{"> " + proposition.texte});

            // Affichage du nouveau lieu et création des boutons des nouvelles propositions
            lieuActuel = lieu;
            initLieu();
        } else {
            // Cas particulier : le lieu est déclarée dans une proposition mais pas encore décrit
            // (lors de l'élaboration de l'aventure par exemple)
            JOptionPane.showMessageDialog(null,"Lieu n° " + proposition.numeroLieu + " à implémenter"); 
        }
    }

    /*
     * Gère l'affichage dans la zone de texte, avec un effet de défilement
     * (comme dans un terminal)
     */
    private void affiche(String[] contenu) {
        int n = contenu.length;
        for (int i = 0; i < nbLignes-(n+1); i++) {
            labels[i].setText(labels[i + n + 1].getText());
        }
        labels[nbLignes-(n+1)].setText(" ");
        for(int i = 0; i<n; i++) {
            labels[nbLignes-n+i].setText(contenu[i]);
        }
    }

}
