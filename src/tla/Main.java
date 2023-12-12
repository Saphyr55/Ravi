package tla;

import tla.syntax.Lexer;
import tla.syntax.Token;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {

        String source = Files.readString(Path.of("game.amp"), StandardCharsets.UTF_8);
        Lexer lexer = new Lexer(source, LinkedList::new);
        List<Token> tokens = lexer.scan();
        System.out.println(Arrays.toString(tokens.toArray()));

        SwingUtilities.invokeLater(new App()::init);
    }

}