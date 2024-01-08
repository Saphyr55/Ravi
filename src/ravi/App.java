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

import ravi.analysis.ast.*;
import ravi.core.NativeDeclaration;
import ravi.infer.Context;
import ravi.infer.Inference;
import ravi.infer.Scheme;
import ravi.infer.Type;
import ravi.model.Application;
import ravi.model.Value;
import ravi.resolver.Environment;
import ravi.resolver.InterpretException;
import ravi.resolver.Interpreter;
import ravi.resolver.ScopeResolver;
import ravi.analysis.Lexer;
import ravi.analysis.Parser;
import ravi.analysis.Token;

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

    static final List<Lieu> LIEUX = new ArrayList<>();
    static final Map<Integer, Lieu> CONTENT = new HashMap<>();

    public static void main(String[] args) throws IOException {

        String source =
                Files.readString(
                    Path.of("ravi/Core.ravi"),
                    StandardCharsets.UTF_8
                );

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Interpreter interpreter = new Interpreter(environment());

        ScopeResolver scopeResolver = new ScopeResolver(interpreter);
        List<Token> tokens = lexer.scan(source);
        Program program = parser.program(tokens);

        Inference inference = new Inference();
        Context context = inference.infer(context(), program);
        System.out.println(context);

        scopeResolver.resolve(program);
        interpreter.interpretProgram(program);

        for (int i = 0; i < LIEUX.size(); i++) {
            CONTENT.put(i, LIEUX.get(i));
        }

        App app = new App();
        SwingUtilities.invokeLater(() -> app.init(0));
    }

    private static Context context() {
        return new Context(Map.of(
                "True", new Scheme(List.of(), new Type.TBool()),
                "False", new Scheme(List.of(), new Type.TBool()),
                "print", new Scheme(List.of("'a"), new Type.TFunc(List.of(new Type.TVar("'a")), new Type.TUnit())),
                "+", new Scheme(List.of(), new Type.TFunc(List.of(new Type.TInt(), new Type.TInt()), new Type.TInt())),
                "-", new Scheme(List.of(), new Type.TFunc(List.of(new Type.TInt(), new Type.TInt()), new Type.TInt())),
                "*", new Scheme(List.of(), new Type.TFunc(List.of(new Type.TInt(), new Type.TInt()), new Type.TInt())),
                "/", new Scheme(List.of(), new Type.TFunc(List.of(new Type.TInt(), new Type.TInt()), new Type.TInt())),
                "=", new Scheme(List.of("'a"), new Type.TFunc(List.of(new Type.TVar("'a"), new Type.TVar("'a")), new Type.TBool())),
                "!=", new Scheme(List.of("'a"), new Type.TFunc(List.of(new Type.TVar("'a"), new Type.TVar("'a")), new Type.TBool()))
        ), Map.of(
                "Float", new Scheme(List.of(), new Type.TFloat()),
                "Unit", new Scheme(List.of(), new Type.TUnit()),
                "Int", new Scheme(List.of(), new Type.TInt()),
                "String", new Scheme(List.of(), new Type.TString()),
                "Bool", new Scheme(List.of(), new Type.TBool())
        ), new Context());
    }

    static Proposition mapValueToProposition(Value value) {
        if (value instanceof Value.VObject object &&
                object.content() instanceof Proposition proposition) {
            return proposition;
        }
        throw new InterpretException("The function 'insert' take a 'Proposition' list as second parameter.");
    }

    static Environment environment() {
        Environment env = new Environment();
        NativeDeclaration.genNative(env);

        env.define("True",new Value.VBool(true));
        env.define("False",new Value.VBool(false));

        env.define("location", Application.value(1, (inter, args) -> {
            var lieux = new Lieu(args.get(0).toStr(), new ArrayList<>());
            LIEUX.add(lieux);
            return Value.object(lieux);
        }));

        env.define("notImplLocation", new Value.VObject(new Lieu("", List.of())));

        env.define("proposition", Application.value(2, (inter, args) -> {
            var lieu = (Lieu) ((Value.VObject) args.get(0)).content();
            var description = args.get(1).toStr();
            return Value.object(new Proposition(description, LIEUX.indexOf(lieu)));
        }));

        env.define("insert", Application.value(2, (inter, args) -> {
            if (((Value.VObject) args.get(0)).content() instanceof Lieu lieu) {
                lieu.propositions.addAll(((Value.VList) args.get(1))
                        .values()
                        .stream()
                        .map(App::mapValueToProposition)
                        .toList());
                return Value.unit();
            }
            throw new InterpretException("The function 'insert' take a 'Lieu' list as first parameter.");
        }));

        return env;
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

    public void init(int start) {

        // Charge le contenu de l'aventure
        lieux = CONTENT;

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
        lieuActuel = lieux.get(start);
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
