package com.yournix.app;

import java.util.Arrays;

/**
 * Матричный шифр Хилла для русского алфавита (33 буквы, арифметика по модулю 33).
 *
 * Шифруются только русские буквы. Пробелы, знаки, цифры и остальные символы остаются на своих местах,
 * регистр букв сохраняется. В конец добавляется от 1 до n служебных букв (дополнение до целого блока,
 * как в PKCS#7). При расшифровке они убираются сами.
 */
public final class HillCipher {

    public static final String ALPHABET = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";
    public static final int MOD = 33;

    /**
     * Ключ 3×3. det = −56 ≡ 10 (mod 33), НОД(10, 33) = 1, поэтому ключ обратим.
     * Чтобы сменить ключ, замените числа: нужно, чтобы НОД(det, 33) = 1. Обратную матрицу программа найдёт сама.
     */
    private static final int[][] DEFAULT_KEY = {
            {2, 1, 5},
            {1, 3, 4},
            {5, 2, 1}
    };

    private final int n;
    private final int[][] key;
    private final int[][] inverse;

    public HillCipher() {
        this(DEFAULT_KEY);
    }

    public HillCipher(int[][] matrix) {
        n = matrix.length;
        if (n < 2) {
            throw new IllegalArgumentException("Матрица ключа должна быть не меньше 2×2");
        }
        key = new int[n][n];
        for (int i = 0; i < n; i++) {
            if (matrix[i].length != n) {
                throw new IllegalArgumentException("Матрица ключа должна быть квадратной");
            }
            for (int j = 0; j < n; j++) {
                key[i][j] = mod(matrix[i][j]);
            }
        }
        inverse = invert(key);

        // Самопроверка: K · K⁻¹ = E (mod 33)
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int s = 0;
                for (int k = 0; k < n; k++) {
                    s += key[i][k] * inverse[k][j];
                }
                if (mod(s) != (i == j ? 1 : 0)) {
                    throw new IllegalStateException("Не удалось построить обратную матрицу");
                }
            }
        }
    }

    public int blockSize() {
        return n;
    }

    /** Шифрует текст. Если в нём нет русских букв, возвращает его без изменений. */
    public String encrypt(String text) {
        int[] codes = new int[text.length()];
        boolean[] lower = new boolean[text.length()];
        int count = scan(text, codes, lower);
        if (count == 0) {
            return text;
        }

        int pad = n - count % n; // от 1 до n
        int total = count + pad;
        int[] plain = new int[total];
        System.arraycopy(codes, 0, plain, 0, count);
        for (int i = count; i < total; i++) {
            plain[i] = pad;
        }

        int[] cipher = apply(key, plain);
        return rebuild(text, lower, count, cipher, total, lower[count - 1]);
    }

    /**
     * Расшифровывает текст.
     *
     * @throws IllegalArgumentException если текст нельзя расшифровать (сообщение готово для показа пользователю)
     */
    public String decrypt(String text) {
        int[] codes = new int[text.length()];
        boolean[] lower = new boolean[text.length()];
        int count = scan(text, codes, lower);
        if (count == 0) {
            return text;
        }
        if (count % n != 0) {
            throw new IllegalArgumentException("Русских букв в шифротексте: " + count + ", а нужно число, кратное "
                    + n + ". Вставьте шифротекст целиком.");
        }

        int[] plain = apply(inverse, Arrays.copyOf(codes, count));
        int pad = plain[count - 1];
        boolean ok = pad >= 1 && pad <= n;
        for (int i = count - pad; ok && i < count; i++) {
            ok = plain[i] == pad;
        }
        if (!ok) {
            throw new IllegalArgumentException("Не удалось расшифровать: текст изменён или зашифрован другим ключом. "
                    + "Вставьте шифротекст целиком, без правок.");
        }
        return rebuild(text, lower, count, plain, count - pad, false);
    }

    // ---------------------------------------------------------------- текст

    /** Номер буквы в алфавите (А = 0 … Я = 32) или −1, если это не русская буква. */
    private static int codeOf(char ch) {
        return ALPHABET.indexOf(Character.toUpperCase(ch));
    }

    private static char letter(int code, boolean lower) {
        char c = ALPHABET.charAt(code);
        return lower ? Character.toLowerCase(c) : c;
    }

    /** Выписывает коды русских букв и их регистр. Возвращает количество букв. */
    private static int scan(String text, int[] codes, boolean[] lower) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int code = codeOf(ch);
            if (code >= 0) {
                codes[count] = code;
                lower[count] = Character.isLowerCase(ch);
                count++;
            }
        }
        return count;
    }

    /**
     * Собирает результат: на места русских букв исходного текста ставит буквы из out (с тем же регистром),
     * остальные символы переносит как есть. Лишние буквы out (дополнение) идут сразу после последней буквы,
     * а лишние буквы исходного текста (дополнение шифротекста) отбрасываются.
     */
    private static String rebuild(String text, boolean[] lower, int srcCount, int[] out, int outCount,
                                  boolean tailLower) {
        StringBuilder sb = new StringBuilder(text.length() + 4);
        int idx = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (codeOf(ch) < 0) {
                sb.append(ch);
                continue;
            }
            if (idx < outCount) {
                sb.append(letter(out[idx], lower[idx]));
            }
            idx++;
            if (idx == srcCount) {
                for (int j = srcCount; j < outCount; j++) {
                    sb.append(letter(out[j], tailLower));
                }
            }
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- математика

    private static int mod(int v) {
        return ((v % MOD) + MOD) % MOD;
    }

    /** Умножает каждый блок из n чисел на матрицу m по модулю 33. */
    private static int[] apply(int[][] m, int[] v) {
        int n = m.length;
        int[] out = new int[v.length];
        for (int b = 0; b < v.length; b += n) {
            for (int i = 0; i < n; i++) {
                int s = 0;
                for (int j = 0; j < n; j++) {
                    s += m[i][j] * v[b + j];
                }
                out[b + i] = s % MOD;
            }
        }
        return out;
    }

    /** Обратная матрица по модулю 33: K⁻¹ = det⁻¹ · adj(K). */
    private static int[][] invert(int[][] a) {
        int n = a.length;
        int d = mod((int) (det(a) % MOD));
        int dInv = -1;
        for (int x = 1; x < MOD; x++) {
            if (d * x % MOD == 1) {
                dInv = x;
                break;
            }
        }
        if (dInv < 0) {
            throw new IllegalArgumentException("Ключ необратим по модулю " + MOD + ": НОД(det, " + MOD + ") не равен 1");
        }
        int[][] inv = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                long cof = det(minor(a, i, j));
                if ((i + j) % 2 == 1) {
                    cof = -cof;
                }
                inv[j][i] = mod((int) (cof % MOD) * dInv);
            }
        }
        return inv;
    }

    private static long det(int[][] a) {
        int n = a.length;
        if (n == 1) {
            return a[0][0];
        }
        long d = 0;
        for (int c = 0; c < n; c++) {
            long term = (long) a[0][c] * det(minor(a, 0, c));
            d += (c % 2 == 0) ? term : -term;
        }
        return d;
    }

    private static int[][] minor(int[][] a, int row, int col) {
        int n = a.length;
        int[][] m = new int[n - 1][n - 1];
        for (int i = 0, mi = 0; i < n; i++) {
            if (i == row) {
                continue;
            }
            for (int j = 0, mj = 0; j < n; j++) {
                if (j == col) {
                    continue;
                }
                m[mi][mj++] = a[i][j];
            }
            mi++;
        }
        return m;
    }
}
