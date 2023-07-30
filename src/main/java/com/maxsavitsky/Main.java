package com.maxsavitsky;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Main {

    private static final char DASH = '–';

    // в некоторых случаях происходит ложное срабатывание на обращения
    // например, "сказал старик Пете"
    // в данном случае алгоритм посчитает автором Петю, хотя в данном случае определить имя невозможно
    private static final List<Integer> IGNORE_AUTHOR_IN_THIS_LINES = List.of(
            // в строке 180 слова автора такие: сказал виконт, не глядя на Пьера
            // в этом случае алгоритм распознаёт, что это слова персонажа по имени "Пьера"
            180,
            // "сказал Екатеринский старик"
            4808,
            // "сказал полковой командир и обратился к майору Экономову"
            2336,
            // "отвечал писарь непочтительно и сердито, оглядываясь на Козловского"
            2124,
            // "вдруг послышался через стол басистый голос Марьи Дмитриевны"
            801, 804,
            // "говорил маленький брат Наташе"
            807,
            // "Приехал в город, вздумал на обед звать, – я ему такой обед задал"
            // в данном случае программа распознаёт последовательность ", -" как отдельяющую
            // слова автора, и всё сбивается
            4449,
            // "сказал старик и тряхнул напудренною головой, сколько позволяла это заплетаемая коса, находившаяся в руках Тихона"
            1184,
            // "говорил, обращаясь к пунцово красному, взволнованному Ростову"
            1692,
            // "Борис встал навстречу Ростову"
            2816,
            // во всех этих случаях слова автора примерно такие "сказал кто-то Ростову",
            // то есть это обращения
            2978, 3101, 3308,
            // "сказал государь, снова взглянув в глаза императору Францу..."
            3202,
            // "обратился граф к Шиншину"
            6165,
            // "послышался ему сзади отчаянный, как ему показалось, шопот княжны Марьи"
            4256,
            // "спросили оба Ростова, старший и младший"
            3522,
            // "Доктор увидал подымающегося на лестницу Ростова"
            4527,
            // "прокричал этот солдат, выкатывая глаза на Ростова"
            4549,
            // "говорил этот человек и увидав Ростова перестал говорить и нахмурился"
            4631,
            // "сказал дежурный штаб офицер, приятно улыбаясь Болконскому"
            2424,
            // "отвечал чей то другой голос, столь человеческий после того нечеловеческого голоса, который сказал: Les huzards de Pavlograd?"
            2984,
            // "отвечал голос Лаврушки"
            1577,
            // "отвечал голос дворецкого Демьяна"
            3720,
            // "говорил приятный и как будто знакомый князю Андрею голос"
            2235,
            // "сказал фейерверкер князю Андрею"
            2366,
            // "сказал шопотом князю Андрею Несвицкий"
            3183,
            // "сказала одна из девушек помощниц няни, обращаясь к князю Андрею"
            4208,
            // "ласково и нежно проговорил тот же голос князя Андрея"
            253,
            // "спрашивали княгиня и княжна, увидев князя Андрея"
            1336,
            // "только слышался крик Балаги"
            6495,
            // "повторил граф, видимо сожалея, что кончилась так скоро речь Семена"
            5508,
            5449,
            // "сказал голос Сони"
            5796, 5861,
            // "спросила маленькая княгиня у Анатоля"
            2637, 2658, 6673,
            // "сказала графиня холодно, как показалось Наташе"
            5321,
            // "говорила самая смелая, прямо уж обращаясь к Наташе"
            5613, 5640,
            // "говорила маленькая княгиня, прощаясь с Анной Павловной"
            238,
            4810, 4671, 3436, 3444, 2316, 1893, 1592,
            // в данном случае это обращения по типу "сказал Андрей Пьеру"
            4400, 3591, 3573, 2258, 1039, 1810,
            1423, 5593,
            // "сказал кто то в свите Багратиона"
            2285, 3256,
            // "сказала графиня, тихо улыбаясь, глядя на мать Бориса"
            500,
            // "говорила графиня вслед за упоминанием о Борисе"
            4966,
            // "говорила княжна, обращаясь к князю Василью"
            1046, 2633, 2666,
            // "Он обнял одной рукой Пьера"
            2536, 4020, 4030, 4443,
            // "заговорил опять масон, глядя не на лицо Пьера"
            4039, 4080,
            // "сказал виконт, обращаясь к Анне Павловне"
            184,
            // "сказала княжна, оглядывая Анну Михайловну, как незнакомую"
            641, 1060,
            // "сказал старый граф, обращаясь к гостье и указывая на своего Николая"
            478, 3920, 5558, 5591,
            2486,
            // "молодой человек обыкновенно из Петербурга приезжает в Москву в отпуск"
            6030,
            // "еще решительнее, не срываясь, прозвучал голосок Наташи"
            813, 3447, 3451, 3764, 5045, 5795, 6209, 6516,
            // "послышался плачущий, не свой голос Илагина"
            5592,
            // "сказал виконт, усевшись в карету с Ипполитом"
            257,
            // "сказал ему денщик Телянина"
            1656,
            // "говорил Илагинский стремянный"
            5597,
            // "Анатоль вздохнул и обнял Долохова"
            1382, 6430
    );

    // в данных случаях из-за пунктуации не удаётся распознавать слова автора и слова персонажа
    private static final List<Integer> IGNORED_LINES = List.of(
            //в строке 6627 файла допущена опечатка редактора: не поставлена запятая, которая по правилам должна разграничать
            // слова прямой речи и слова автора (- П, - а.)
            6627,
            4423,
            6322,
            4811,
            2892,
            6107,
            4598,
            1836,
            582,
            333,
            5715,
            // "– Вы будете отвечать, ротмистр, это буйство, – у своих транспорты отбивать! Наши два дня не ели"
            4497
    );

    private static final Pattern SUBCHAPTER_PATTERN = Pattern.compile("[IVXLCDM]+");
    private static final Pattern HE_SHE_PATTERN = Pattern.compile(" она?(,| )");

    private static final List<Replica> allReplicas = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        File souceFile = new File("war_and_peace.txt");
        List<String> lines = Files.lines(souceFile.toPath())
                .toList();

        splitIntoReplicas(lines);

        BufferedWriter writer = new BufferedWriter(new FileWriter("filtered_characters.txt"));
        for(String s : new HashSet<>(allReplicas.stream().map(r -> r.authorName).toList())) {
            writer.write(s + "\n");
        }
        writer.flush();
        writer.close();
    }

    private static void splitIntoReplicas(List<String> lines) {
        int currentVolume = 0;
        int currentChapter = 0;
        //for(int i = 13; i < 20; i++){
        for(int i = 13; i < lines.size(); i++){
            String s = lines.get(i);
            if(s.isEmpty())
                continue;
            if(SUBCHAPTER_PATTERN.matcher(s).matches()) // игнорируем строки, которые обозначают подглавы
                continue;

            if(s.startsWith("Том")) {
                currentVolume++;
                continue;
            }
            if(s.startsWith("ЧАСТЬ")){
                currentChapter++;
                continue;
            }

            if(!s.startsWith(String.valueOf(DASH)))
                continue;

            if(IGNORED_LINES.contains(i + 1))
                continue;

            List<Replica> replicas = getReplicasInLine(s, i + 1);
            for(Replica r : replicas){
                r.chapter = currentChapter;
                r.volume = currentVolume;
            }
            allReplicas.addAll(replicas);
        }
    }

    private static List<Replica> getReplicasInLine(String line, int lineIndex){
        List<Replica> replicas = new ArrayList<>();
        boolean isAuthorWords = false;
        String name = null; // имя того, кто произносит слова
        int partStartIndex = 0;
        final boolean ignoreAuthor = IGNORE_AUTHOR_IN_THIS_LINES.contains(lineIndex);
        for(int i = 1; i < line.length(); i++){
            if(isJointOfSpeech(i, line)){
                isAuthorWords = !isAuthorWords;
                if(line.charAt(i + 1) == ']')
                    i++;
                i += 3;
                partStartIndex = i + 1;
                continue;
            }
            if(isAuthorWords && name == null && !ignoreAuthor){
                // в данном случае наиболее вероятным будет тот случай, когда имя персонажа будет написано самыым первым
                // Пример: - ... - сказал Петя, - ...

                // сдвигать нужно, чтобы избежать ложных случаев распознавания
                // например: - ... - Сказал он...
                i += 1;

                while (i < line.length()
                        && !isJointOfSpeech(i, line)
                        && line.charAt(i) != '.'
                        && line.charAt(i) != '…'
                        && !Character.isUpperCase(line.charAt(i))) {
                    i++;
                }
                if(i == line.length())
                    break;
                if(line.charAt(i) == '.' || line.charAt(i) == '…'){
                    // в этом случае нужно пропускать следующие предложения, так как имя говорящего персонажа
                    // может быть только в первом предложении.
                    while(i < line.length() && !isJointOfSpeech(i, line))
                        i++;
                    i--;
                    continue;
                }
                if(isJointOfSpeech(i, line)){
                    isAuthorWords = false;
                    if(line.charAt(i + 1) == ']')
                        i++;
                    i += 3;
                    partStartIndex = i + 1;
                    continue;
                }

                // чтобы не было случаев "сказал он Пьеру"
                if(HE_SHE_PATTERN.matcher(line.substring(partStartIndex, i)).find()){
                    while(i < line.length() && !isJointOfSpeech(i, line))
                        i++;
                    i--;
                    continue;
                }

                int j = i;
                // имя может состоять из нескольких частей. Например, Андрей Болконский
                while(true) {
                    for (; j < line.length() && Character.isAlphabetic(line.charAt(j)); j++);
                    if(j == line.length() || line.charAt(j) != ' ')
                        break;
                    // значит charAt(j) == ' '
                    if (j + 1 == line.length() || !Character.isUpperCase(line.charAt(j + 1))) {
                        break;
                    }
                    j++;
                }
                name = line.substring(i, j);
                i = j - 1;
                continue;
            }
            if(!isAuthorWords){
                int j = i;
                while(j < line.length() && !isJointOfSpeech(j, line))
                    j++;
                int endIndex = j;
                if(endIndex != line.length()){
                    // учитываем знак препинания, а также скобку, закрывающую перевод французских слов
                    endIndex++;
                    if(line.charAt(j) == ']')
                        endIndex++;
                }
                replicas.add(new Replica(line.substring(i, endIndex), lineIndex));
                i = j - 1;
            }
        }
        // имён, которые заканчиваются на "у" в И.п., нет, так что скорее всего это обращение
        if(name != null && !name.contains(" ") && (name.endsWith("у") || name.endsWith("ых")))
            name = null;
        for(Replica r : replicas)
            r.authorName = name == null ? "undefined" : name;

        return replicas;
    }

    private static boolean isJointOfSpeech(int i, String line){
        if(i + 4 <= line.length()){
            String s = line.substring(i, i + 4);
            if(s.equals(".] –")
                    || s.equals(",] –")
                    || s.equals("!] –")
                    || s.equals("…] –")
                    || s.equals("?] –"))
                return true;
        }
        if(i + 3 > line.length())
            return false;
        String s = line.substring(i, i + 3);
        return s.equals(". –")
                || s.equals(", –")
                || s.equals("! –")
                || s.equals("? –")
                || s.equals("… –")
                || s.equals("; –")
                || (s.equals(": –") && !line.startsWith("или", i - 3));
    }

    private static class Replica {
        private final int lineIndex;
        private final String text;
        private String authorName;
        private int chapter;
        private int volume;

        public Replica(String text, int lineIndex) {
            this.text = text;
            this.lineIndex = lineIndex;
        }
    }

}