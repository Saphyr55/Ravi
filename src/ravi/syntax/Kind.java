package ravi.syntax;

public enum Kind {


    EOF,
    Unknown,
    NewLine,

    // Literal
    String,
    Text,
    Identifier,
    Number,

    // Symbol
    OpenSquareBracket,
    CloseSquareBracket,
    Comma,
    Equal,
    Semicolon,
    Colon,
    DoubleColon,
    OpenParenthesis,
    CloseParenthesis,
    Pipe,

    // Keywords
    OpenKw,
    IncludeKw,
    NativeKw,
    MatchKw,
    WithKw,
    ValKw,
    TypeKw,
    FunKw,
    TryKw,
    RecKw,
    AndKw,
    DoKw,
    DoneKw,
    ForKw,
    WhileKw,
    IfKw,
    ThenKw,
    ElseKw,
    LetKw,
    BeginKw,
    EndKw,
    InKw,
}
