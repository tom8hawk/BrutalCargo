package ru.baronessdev.personal.brutalcargo;

import lombok.Getter;

public enum WordDeclensionUtil {
    MINUTES(new String[] { "минуту", "минуты", "минут" }),
    SECONDS(new String[] { "секунду", "секунды", "секунд" });

    @Getter
    private final String[] declensions;

    WordDeclensionUtil(String[] declensions) {
        this.declensions = declensions;
    }

    /** Выбирает правильную форму существительного в зависимости от числа.
    * Чтобы легко запомнить, в каком порядке указывать варианты, пользуйтесь мнемоническим правилом:
    * один-два-пять - один гвоздь, два гвоздя, пять гвоздей.

    * @param number Число, по которому идёт склонение.
    * @return Cтрока (число + существительное в нужном падеже).
    */
    public String getWordInDeclension(long number) {
        String str = number + " ";
        number = Math.abs(number);

        if (number % 10 == 1 && number % 100 != 11) {
            str += declensions[0];
        } else if (number % 10 >= 2 && number % 10 <= 4 && (number % 100 < 10 || number % 100 >= 20)) {
            str += declensions[1];
        } else {
            str += declensions[2];
        }

        return str;
    }
}
