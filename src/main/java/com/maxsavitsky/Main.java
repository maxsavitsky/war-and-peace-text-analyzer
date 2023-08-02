package com.maxsavitsky;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
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
            // в данном случае программа распознаёт последовательность ", -" как отделяющую
            // слова автора, и всё сбивается
            4449,
            // "сказал старик и тряхнул напудренною головой, сколько позволяла это заплетаемая коса, находившаяся в руках Тихона"
            1184,
            // "говорил, обращаясь к пунцово красному, взволнованному Ростову"
            1692,
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
            1382
    );

    // в данных случаях из-за пунктуации не удаётся распознавать слова автора и слова персонажа
    private static final List<Integer> IGNORED_LINES = List.of(
            //в строке 6627 файла допущена опечатка редактора: не поставлена запятая,
            // которая по правилам должна разграничивать
            // слова прямой речи и слова автора (- П, - а.)
            6627,
            // "– Вы будете отвечать, ротмистр, это буйство, – у своих транспорты отбивать! Наши два дня не ели"
            4497,
            4423,
            6322,
            4811,
            2892,
            6107,
            4598,
            1836,
            582,
            333,
            5715
    );

    private static final Pattern SUBCHAPTER_PATTERN = Pattern.compile("[IVXLCDM]+");
    private static final Pattern HE_SHE_PATTERN = Pattern.compile(" она?(,| )");
    private static final Pattern FRENCH_WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

    private static final int NUMBER_OF_LINES_TO_SKIP_IN_THE_BEGINNING = 13;
    private static final int NUMBER_OF_LINES_TO_SKIP_IN_THE_ENDING = 9;

    private static final List<Replica> allReplicas = new ArrayList<>();
    private static final List<Author> authors = new ArrayList<>();

    private static final Author EMPTY_AUTHOR = new Author(List.of(), -1);

    private static final Map<String, List<Replica>> separatedByNamesReplicasMap = new HashMap<>();
    private static final Map<Integer, List<Replica>> separatedByIdsReplicasMap = new HashMap<>();

    private static final Map<Integer, Integer> countOfAllocutionsToAuthorMap = new HashMap<>();
    private static final Map<Integer, Integer> wordsCountOfAuthorMap = new HashMap<>();
    private static final Map<Integer, Integer> frenchWordsCountOfAuthorMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        File souceFile = new File("war_and_peace.txt");
        List<String> lines = Files.lines(souceFile.toPath())
                .toList();

        File charactersFile = new File("characters_and_their_synonyms.txt");
        List<String> allLines = Files.readAllLines(charactersFile.toPath());
        List<List<String>> namesList = new ArrayList<>();
        for (String line : allLines) {
            String[] names = line.split(",");
            namesList.add(Arrays.asList(names));
        }

        namesList.sort(Comparator.comparing(o -> o.get(0)));
        for(int i = 0; i < namesList.size(); i++){
            authors.add(new Author(namesList.get(i), i));
        }

        splitIntoReplicas(lines);

        printSortedNames();

        analyzeText();
    }

    private static void splitIntoReplicas(List<String> lines) {
        int currentVolume = 0;
        int currentChapter = 0;
        //for(int i = 13; i < 20; i++){
        for(int i = NUMBER_OF_LINES_TO_SKIP_IN_THE_BEGINNING; i < lines.size() - NUMBER_OF_LINES_TO_SKIP_IN_THE_ENDING; i++){
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
            for(Replica replica : replicas){
                replica.chapter = currentChapter;
                replica.volume = currentVolume;
                separatedByNamesReplicasMap.computeIfAbsent(replica.authorName, k -> new ArrayList<>())
                        .add(replica);
                separatedByIdsReplicasMap.computeIfAbsent(replica.authorId, k -> new ArrayList<>())
                        .add(replica);
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
        for(int i = 2; i < line.length(); i++){
            if(isJointOfSpeech(i, line)){
                isAuthorWords = !isAuthorWords;
                if(line.charAt(i + 1) == ']')
                    i++;
                i += 3;
                partStartIndex = i + 1;
                continue;
            }
            if(isAuthorWords && name == null && !ignoreAuthor){
                int startIndex = i;
                // в данном случае наиболее вероятным будет тот случай, когда имя персонажа будет написано самыым первым
                // Пример: - ... - сказал Петя, - ...

                // сдвигать нужно, чтобы избежать ложных случаев распознавания
                // например: - ... - Сказал он...
                //i += 1;

                // Сдвиг делать больше не надо, так как имя может быть первым
                // например: - ... - Наташа обратилась к ней.
                // но в этот раз полученный результат будет проверяться по списку уже известных имён

                while (i < line.length()
                        && !isJointOfSpeech(i, line)
                        && line.charAt(i) != '.'
                        && line.charAt(i) != '…'
                        && line.charAt(i) != ';'
                        && !Character.isUpperCase(line.charAt(i))) {
                    i++;
                }
                if(i == line.length())
                    break;
                if(line.charAt(i) == '.' || line.charAt(i) == '…' || line.charAt(i) == ';'){
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
                // значит слова автора начинаются с заглавной буквы
                // в данном случае необходимо проверить, что это именно имя
                if(i == startIndex){
                    if(!isAuthorNameExists(name)){
                        name = null;
                        i = startIndex;
                        continue;
                    }
                }
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
                    if(line.charAt(endIndex) == ']')
                        endIndex++;
                }
                replicas.add(new Replica(line.substring(i, endIndex), lineIndex));
                i = j - 1;
            }
        }
        // имён, которые заканчиваются на "у" в И.п., нет, так что скорее всего это обращение
        if(name != null && !name.contains(" ") && (name.endsWith("у") || name.endsWith("ых")))
            name = null;
        if(name == null)
            name = "undefined";
        int authorId = getAuthorId(name);
        for(Replica replica : replicas) {
            System.out.println(name);
            replica.authorName = name;
            replica.authorId = authorId;
        }

        return replicas;
    }

    private static boolean isAuthorNameExists(String name){
        return authors.stream()
                .anyMatch(a -> a.containsName(name));
    }

    private static int getAuthorId(String name){
        return authors.stream()
                .filter(author -> author.containsName(name))
                .findFirst()
                .orElse(EMPTY_AUTHOR)
                .id;
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

    private static void printSortedNames() throws IOException {
        System.out.println("Имена персонажей по возрастанию:");
        try(BufferedWriter writer = new BufferedWriter(new FileWriter("sorted_names.txt"))) {
            for (Author author : authors) {
                if(author.id == 3)
                    continue;
                System.out.println(author.getFormattedName());
                writer.write(author.getFormattedName() + "\n");
            }
        }
        System.out.println("Имена экспортированы в файл sorted_names.txt");
    }

    private static void analyzeText() throws IOException {
        analyzeTotalCountOfWords();
        analyzeReplicas();
    }

    private static void analyzeTotalCountOfWords() throws IOException {
        File sourceFile = new File("war_and_peace.txt");
        List<String> list = Files.lines(sourceFile.toPath())
                .skip(NUMBER_OF_LINES_TO_SKIP_IN_THE_BEGINNING)
                .toList();

        int totalCount = 0;

        Pattern pattern = Pattern.compile("^([IVXLCDM]+|Том|ЧАСТЬ)");
        for (int i = 0; i < list.size() - NUMBER_OF_LINES_TO_SKIP_IN_THE_ENDING; i++) {
            String s = list.get(i);
            if (s.isEmpty() || pattern.matcher(s).matches())
                continue;

            // здесь будет строка без вставок
            StringBuilder sb = new StringBuilder();
            int pos = s.startsWith(DASH + " ") ? 2 : 0;
            while (true) {
                int p = s.indexOf('[', pos);
                if (p == -1) {
                    sb.append(s.substring(pos));
                    break;
                }
                sb.append(s, pos, Math.max(p - 1, 0));
                p = s.indexOf(']', p);
                if (p == -1)
                    break;
                pos = p + 1;
            }

            int count = getCountOfWords(sb.toString());
            totalCount += count;
        }
        System.out.println("Общее кол-во слов романа: " + totalCount);
    }

    private static void analyzeReplicas(){
        analyzeCountOfWordsInReplicas();
        analyzeAllocutions();
    }

    private static void analyzeCountOfWordsInReplicas(){
        int wordsCount = 0;
        int frenchWordsCount = 0;
        for(Replica replica : allReplicas) {
            int authorId = replica.authorId;
            System.out.println(authorId);
            int count = getCountOfWords(replica.textWithoutInserts);
            wordsCountOfAuthorMap.put(authorId, count + wordsCountOfAuthorMap.getOrDefault(authorId, 0));
            wordsCount += count;

            count = getCountOfFrenchWords(replica.textWithoutInserts);
            frenchWordsCountOfAuthorMap.put(authorId, count + frenchWordsCountOfAuthorMap.getOrDefault(authorId, 0));
            frenchWordsCount += count;
        }
        System.out.println("\nОбщий объем в словах прямой речи (без учёта вставок с переводом): " + wordsCount);
        System.out.printf("Из них %d слов на французском%n", frenchWordsCount);

        var list = new ArrayList<>(wordsCountOfAuthorMap.entrySet());
        list.sort((o1, o2) -> Integer.compare(o2.getValue(), o1.getValue()));
        System.out.println("\nПерсонажи и кол-во сказанных ими слов (общее кол-во/на русском/на французском):");
        try(FileWriter fw = new FileWriter("analysis_data/words_count_per_character.csv")) {
            for (var entry : list) {
                String authorName = authors.get(entry.getKey()).getFormattedName();
                int totalCount = entry.getValue();
                int frenchCount = frenchWordsCountOfAuthorMap.get(entry.getKey());
                System.out.println(authorName + ": " + totalCount + ", " + (totalCount - frenchCount) + ", " + frenchCount);
                fw.write("\"%s\",%d,%d,%d\n".formatted(authorName, totalCount, totalCount - frenchCount, frenchCount));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Данные экспортированы в файл analysis_data/words_count_per_character.csv");
    }

    private static void analyzeAllocutions(){
        for(Replica replica : allReplicas){
            String[] parts = replica.textWithoutInserts.split(",");
            for(String part : parts){
                int id = getAuthorId(part.trim());
                if(id == -1)
                    continue;
                countOfAllocutionsToAuthorMap.put(
                        id,
                        countOfAllocutionsToAuthorMap.getOrDefault(id, 0) + 1
                );
            }
        }
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(countOfAllocutionsToAuthorMap.entrySet());
        list.sort((o1, o2) -> Integer.compare(o2.getValue(), o1.getValue()));

        System.out.println("\nКол-во обращений к персонажам (по убыванию):");
        try(FileWriter fw = new FileWriter("analysis_data/count_of_allocutions.csv")) {
            for (var entry : list) {
                String authorName = authors.get(entry.getKey()).getFormattedName();
                System.out.println(authorName + " " + entry.getValue());
                fw.write("\"" + authorName + "\"," + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Данные экспортированы в файл analysis_data/count_of_allocutions.csv");
    }

    private static int getCountOfWords(String text){
        int count = 0;
        char[] charArray = text.trim().toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];
            if (c == ' ' && i + 1 < charArray.length
                    && charArray[i + 1] != DASH && charArray[i + 1] != ' ')
                count++;
        }

        return count + 1;
    }

    private static int getCountOfFrenchWords(String text){
        String[] words = text.trim().split(" ");
        int count = 0;
        for(String word : words){
            if(FRENCH_WORD_PATTERN.matcher(word).find())
                count++;
        }
        return count;
    }

    private static class Replica {
        private final int lineIndex;
        private final String text;
        // текст без переводов французских слов
        private final String textWithoutInserts;
        private String authorName;
        private int authorId;
        private int chapter;
        private int volume;

        public Replica(String text, int lineIndex) {
            this.text = text;
            this.textWithoutInserts = removeInserts(text);
            this.lineIndex = lineIndex;
        }

        private String removeInserts(String line){
            StringBuilder sb = new StringBuilder();
            boolean insertStarted = false;
            for(int i = 0; i < line.length(); i++){
                if(line.charAt(i) == ' ' && line.charAt(i + 1) == '['){
                    insertStarted = true;
                }else if(line.charAt(i) == ']'){
                    insertStarted = false;
                }else if(!insertStarted){
                    sb.append(line.charAt(i));
                }
            }
            return sb.toString();
        }
    }

    private static class Author {
        private final List<String> synonyms;
        private final int id;

        public Author(List<String> synonyms, int id) {
            this.synonyms = synonyms;
            this.id = id;
        }

        public boolean containsName(String name){
            return synonyms.contains(name);
        }

        public String getFormattedName(){
            String result = synonyms.get(0);
            if(synonyms.size() > 1){
                result += " ("
                        + String.join(", ", synonyms.subList(1, synonyms.size()))
                        + ")";
            }
            return result;
        }

    }

}