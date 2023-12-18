package ravi.analysis;

public enum Kind {

    EOF,
    Unknown,

    // Literal
    String,
    Text,
    CapitalizedIdentifier,
    LowercaseIdentifier,
    Int,
    Float,

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
    Dot,
    Operator,

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
