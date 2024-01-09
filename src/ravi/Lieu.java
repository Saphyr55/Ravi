package ravi;

import ravi.model.Value;

import java.util.List;

/*
 * Lieu dans l'aventure.
 * 
 * Composée d'une description et d'aucune, une ou plusieurs propositions.
 */
public class Lieu {

    Value.VApplication application;
    String description;
    List<Proposition> propositions;

    public Lieu(String description, List<Proposition> propositions) {
        this.description = description;
        this.propositions = propositions;
        this.application = Value.application((inter, args) -> Value.unit());
    }

    @Override
    public String toString() {
        return "Lieu{" +
                "description='" + description + '\'' +
                ", propositions=" + propositions +
                '}';
    }
}
