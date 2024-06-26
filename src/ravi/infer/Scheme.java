package ravi.infer;

import java.util.*;

public record Scheme(List<String> forall, Type type) implements Typing<Scheme> {

    public Scheme(List<String> forall, Type type) {
        this.forall = List.copyOf(forall);
        this.type = type;
    }

    @Override
    public Set<String> ftv() {
        Set<String> ftv = new HashSet<>(type.ftv());
        forall().forEach(ftv::remove);
        return ftv;
    }

    @Override
    public Scheme apply(Substitution s) {
        List<String> vars = new LinkedList<>(forall());
        var st = new HashMap<>(s.types());
        for (int i = vars.size() - 1; i >= 0; i--) {
            st.remove(vars.get(i));
        }
        return new Scheme(vars, type.apply(new Substitution(st)));
    }

    @Override
    public String toString() {
        return forall.isEmpty() ? type.toStr() : "forall " + String.join(" ", forall) + ". " + type.toStr();
    }


}
