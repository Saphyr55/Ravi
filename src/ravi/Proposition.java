package ravi;

import ravi.model.Value;

/*
 * Proposition faite au joueur pour poursuivre dans l'aventure.
 * 
 * Une proposition mène à un nouveau lieu, identifié par un numéro.
 */
public class Proposition {
    String texte;
    int numeroLieu;
    Value.VApplication application;

    public Proposition(String texte, int numeroLieu) {
        this.texte = texte;
        this.numeroLieu = numeroLieu;
        this.application = Value.application((inter, args) -> Value.unit());
    }

    @Override
    public String toString() {
        return "Proposition{" +
                "texte='" + texte + '\'' +
                ", numeroLieu=" + numeroLieu +
                '}';
    }
}
