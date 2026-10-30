package com.mycomplex.users.infrastructure.adapter.out.auth0;

import java.security.SecureRandom;

final class SecureTemporaryPasswordGenerator {

    private static final char[] LOWER =
            "abcdefghijkmnopqrstuvwxyz".toCharArray();
    private static final char[] UPPER =
            "ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final char[] DIGITS = "23456789".toCharArray();
    private static final char[] SYMBOLS = "!@#$%*-_+=".toCharArray();
    private static final char[] ALL = (
            new String(LOWER) + new String(UPPER)
                    + new String(DIGITS) + new String(SYMBOLS)
    ).toCharArray();

    private final SecureRandom random = new SecureRandom();

    String generate() {
        char[] password = new char[24];
        password[0] = choose(LOWER);
        password[1] = choose(UPPER);
        password[2] = choose(DIGITS);
        password[3] = choose(SYMBOLS);
        for (int index = 4; index < password.length; index++) {
            password[index] = choose(ALL);
        }
        for (int index = password.length - 1; index > 0; index--) {
            int other = random.nextInt(index + 1);
            char current = password[index];
            password[index] = password[other];
            password[other] = current;
        }
        return new String(password);
    }

    private char choose(char[] characters) {
        return characters[random.nextInt(characters.length)];
    }
}
