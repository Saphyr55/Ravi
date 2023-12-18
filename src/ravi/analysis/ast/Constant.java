package ravi.analysis.ast;

public sealed interface Constant {

    record CInt(Integer integer) implements Constant { }
    record CFloat(Float cFloat) implements Constant { }

    record CText(String content) implements Constant { }

    record CString(String content) implements Constant { }

    record CEmptyList() implements Constant { }

    record CUnit() implements Constant { }

}
