package ravi.syntax;

public enum Kind {

    EOF,
    Unknown,

    // Literal
    String,
    Text,
    CapitalizedIdentifier,
    LowercaseIdentifier,
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
    Arrow,
    Minus,
    Dot,

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
    WhenKw,
    ModuleKw,


}
