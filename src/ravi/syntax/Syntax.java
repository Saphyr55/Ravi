package ravi.syntax;


import ravi.core.Bind;

public final class Syntax {

    public final static class Word {

        @Bind(kind = Kind.WithKw)
        public static final String With = "with";

        @Bind(kind = Kind.MatchKw)
        public static final String Match = "match";

        @Bind(kind = Kind.NativeKw)
        public static final String Native = "native";

        @Bind(kind = Kind.IncludeKw)
        public static final String Include = "include";

        @Bind(kind = Kind.TypeKw)
        public static final String Type = "type";

        @Bind(kind = Kind.OpenKw)
        public static final String Open = "open";

        @Bind(kind = Kind.LetKw)
        public static final String Let = "let";

        @Bind(kind = Kind.ThenKw)
        public static final String Then = "then";

        @Bind(kind = Kind.ElseKw)
        public static final String Else = "else";

        @Bind(kind = Kind.IfKw)
        public static final String If = "if";

        @Bind(kind = Kind.InKw)
        public static final String In = "in";

        @Bind(kind = Kind.BeginKw)
        public static final String Begin = "begin";

        @Bind(kind = Kind.EndKw)
        public static final String End = "end";

        @Bind(kind = Kind.ValKw)
        public static final String Val = "val";

        @Bind(kind = Kind.FunKw)
        public static final String Fun = "fun";

        @Bind(kind = Kind.TryKw)
        public static final String Try = "try";

        @Bind(kind = Kind.RecKw)
        public static final String Rec = "rec";

        @Bind(kind = Kind.AndKw)
        public static final String And = "and";

        @Bind(kind = Kind.DoKw)
        public static final String Do = "do";

        @Bind(kind = Kind.DoneKw)
        public static final String Done = "done";

        @Bind(kind = Kind.ForKw)
        public static final String For = "for";

        @Bind(kind = Kind.WhileKw)
        public static final String While = "while";

        @Bind(kind = Kind.WhenKw)
        public static final String When = "when";
    }

    public final static class Symbol {
        public static final String Minus = "-";
        public static final String Space = " ";
        public static final String Equal = "=";
        public static final String CloseSquareBracket = "]";
        public static final String OpenSquareBracket = "[";
        public static final String BackslashN = "\n";
        public static final String BackslashT = "\t";
        public static final String BackslashR = "\r";
        public static final String DoubleQuote = "\"";
        public static final String Comma = ",";
        public static final String Semicolon = ";";
        public static final String OpenParenthesis = "(";
        public static final String CloseParenthesis = ")";
        public static final String Colon = ":";
        public static final String Pipe = "|";
        public static final String Arrow = ">";
    }


}
