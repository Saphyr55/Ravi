package tla;

import java.util.List;

/*
 * Lieu dans l'aventure.
 * 
 * Composée d'une description et d'aucune, une ou plusieurs propositions.
 */
public class Lieu {
    String description;
    List<Proposition> propositions;

    public Lieu(String description, List<Proposition> propositions) {
        this.description = description;
        this.propositions = propositions;
    }
}
