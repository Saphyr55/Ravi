package tla.syntax;


import tla.core.Bind;

public final class Syntax {

    public final static class Word {

        @Bind(kind = Kind.LetKw)
        public static final String Let = "let";
        @Bind(kind = Kind.ElseKw)
        public static final String Else = "else";
        @Bind(kind = Kind.IfKw)
        public static final String If = "if";
        @Bind(kind = Kind.InKw)
        public static final String In = "in";
        @Bind(kind = Kind.InsertKw)
        public static final String insert = "insert";

        @Bind(kind = Kind.BeginKw)
        public static final String Begin = "begin";
        @Bind(kind = Kind.EndKw)
        public static final String End = "end";

        @Bind(kind = Kind.DirectKw)
        public static final String Direct = "direct";

        @Bind(kind = Kind.ThenKw)
        public static final String Then = "then";

        @Bind(kind = Kind.ToKw)
        public static final String To = "to";

        @Bind(kind = Kind.PropositionKw)
        public static final String Proposition = "proposition";

        @Bind(kind = Kind.LocationKw)
        public static final String Location = "location";
    }

    public final static class Symbol {
        public static final String Space = " ";
        public static final String CloseSquareBracket = "]";
        public static final String OpenSquareBracket = "[";
        public static final String BackslashN = "\n";
        public static final String BackslashT = "\t";
        public static final String BackslashR = "\r";
        public static final String DoubleQuote = "\"";
        @Bind(kind = Kind.ThreeDoubleQuote, pred = DoubleQuote)
        public static final String ThreeDoubleQuote = "\"\"\"";
        public static final String Comma = ",";
    }


}
