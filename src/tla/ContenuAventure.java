package tla;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Contenu de l'aventure.
 * 
 * La HashMap lieux attribue un numéro à chaque lieu qui compose l'aventure.
 * 
 * Ces textes proviennent de l'aventure écrite par de Denis Gerfaud,
 * "Le mystère de la statue maudite", parue dans le numéro 31 (février/mars 1985)
 * du magazine Jeux et Stratégie, page 66 à 69
 */
public class ContenuAventure {

    final public static String titre = "Le mystère de la statue maudite";

    static Map<Integer, Lieu> init() {
        HashMap<Integer, Lieu> lieux = new HashMap<>();

        lieux.put(1, new Lieu(
            "Vous reprenez conscience.\nVous êtes dans un souterrain en partie éboulé.\nL'un des bouts de souterrain se prolonge au-delà de la portée de votre lampe;\nTout près de vous, des pierres et des gravats bloquent entièrement le passage.\nIl faut maintenant sortir de là !",
            List.of(
                new Proposition("Tentez-vous de dégager l'éboulement pour éventuellement ressortir par où avez dû entrer ?", 13),
                new Proposition("Explorez-vous le souterrain dans l'autre direction ?", 9)
            )
        ));

        lieux.put(6, new Lieu(
            "Au bout de quelques vingt ou trente mètres, le couloir se termine en cul-de-sac à cause d'un\néboulement.",
            List.of(
                new Proposition("Faites-vous demi-tour ?", 27),
                new Proposition("Décidez-vous de vous frayez un passage à travers les gravats ?", 16)
            )
        ));

        lieux.put(9, new Lieu(
            "Quelque vingt mètres plus loin, vous arrivez au pied d'un escalier.\nQue faites-vous ?",
            List.of(
                new Proposition("Vous examinez la fresque ?", 20),
                new Proposition("Vous empruntez le couloir en face de l'escalier ?", 18),
                new Proposition("Vous tournez à droite pour explorer le prolongement de la salle ?", 2)
            )
        ));

        lieux.put(13, new Lieu(
            "L'éboulement est récent, et les gravats instables.\nDans un soudain nuage de poussière, une pluie de pierres vous ensevelit.\nPerdez 2 points de survie. Si vous pouvez les dépenser, vous vous dégagez de la nouvelle avalanche.\nQuelle est ensuite votre décision ?",
            List.of(
                new Proposition("Vous continuez à tenter de déblayer ?", 17),
                new Proposition("Vous abandonnez et commencez à explorer le couloir dans l'autre sens ?", 9)
            )
        ));

        lieux.put(17, new Lieu(
            "Une nouvelle avalanche de gravats se déverse sur vous.",
            List.of(
                new Proposition("Vous continuez coûte que coûte à déblayer ?", 13),
                new Proposition("Vous baissez les bras et commencez à explorer le couloir dans l'autre sens ?", 9)
            )
        ));

        lieux.put(18, new Lieu(
            "Une quinzaine de mètre plus loin, le couloir est creusé de deux alcôves",
            List.of(
                new Proposition("Vous les examinez de près ?", 11),
                new Proposition("Vous continuez votre chemin ?", 6)
            )
        ));

        return lieux;
    }
}
