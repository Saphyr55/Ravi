package ravi.syntax.ast;

public sealed interface Constant {

    record CNumber(Number number) implements Constant { }

    record CText(String content) implements Constant { }

    record CString(String content) implements Constant { }

    record CEmptyList() implements Constant { }

    record CUnit() implements Constant { }

}
