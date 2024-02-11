package com.example.demoPokemon.controller;

import com.example.demoPokemon.repository.HistoricalPokemon;
import com.example.demoPokemon.repository.HistoricalResult;
import com.example.demoPokemon.domain.Pokemon;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Controller
public class PokemonController {

    private static final Logger logger = LoggerFactory.getLogger(PokemonController.class);

    private final List<Pokemon> serverPokemon = new ArrayList<>(List.of(
            new Pokemon("Чармандер", "Огненый", 1),
            new Pokemon("Сквиртл", "Водный", 1),
            new Pokemon("Бульбазавр", "Травяной", 1),
            new Pokemon("Гусеница", "Насекомое", 1),
            new Pokemon("Пиджеотто", "Летающий", 1)
    ));

    private final List<Pokemon> userPokemon = new ArrayList<>(List.of(new Pokemon("Pikachu", "Electric", 5)));
    private final List<Pokemon> leaderPokemon = new ArrayList<>();
    private int pikachuLevelCounter = 0;
    private boolean rocketButtonActivated = false;
    private int rocketButtonCounter;

    // Новый список для хранения стадий эволюции
    private final List<List<Pokemon>> evolutionStages = new ArrayList<>();

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("userPokemon", userPokemon);
        model.addAttribute("serverPokemon", serverPokemon);
        model.addAttribute("leader", leaderPokemon.isEmpty() ? null : leaderPokemon.get(0));
        model.addAttribute("rocketButtonActivated", rocketButtonActivated);
        return "index";
    }

    @PostMapping("/catchPokemon")
    public String catchPokemon(Model model) {
        try {
            if (rocketButtonActivated) {
                model.addAttribute("rocketButtonActivated", true);
                return "index";
            }

            Random random = new Random();
            int randomIndex = random.nextInt(serverPokemon.size());
            Pokemon caughtPokemon = serverPokemon.get(randomIndex);

            if (userPokemon.stream().anyMatch(p -> p.getName().equals(caughtPokemon.getName()))) {
                Pokemon existingPokemon = userPokemon.stream()
                        .filter(p -> p.getName().equals(caughtPokemon.getName()))
                        .findFirst()
                        .orElseThrow();

                if (existingPokemon.getLevel() < 100) {
                    existingPokemon.setLevel(existingPokemon.getLevel() + 1);
                }

                if (existingPokemon.getLevel() >= 5 && !leaderPokemon.contains(existingPokemon)) {
                    leaderPokemon.add(existingPokemon);
                    pikachuLevelCounter = 0;
                }
            } else {
                userPokemon.add(new Pokemon(caughtPokemon.getName(), caughtPokemon.getElement(), 1));
            }

            pikachuLevelCounter++;
            if (userPokemon.stream().anyMatch(p -> p.getName().equals("Pikachu")) && pikachuLevelCounter % 5 == 0) {
                userPokemon.stream()
                        .filter(p -> p.getName().equals("Pikachu"))
                        .findFirst()
                        .ifPresent(p -> p.setLevel(p.getLevel() + 1));
            }

            updateLeader();

            model.addAttribute("userPokemon", userPokemon);
            model.addAttribute("serverPokemon", serverPokemon);
            model.addAttribute("leader", leaderPokemon.isEmpty() ? null : leaderPokemon.get(0));

            if (userPokemon.stream().allMatch(p -> p.getLevel() <= 0)) {
                leaderPokemon.clear();
                leaderPokemon.add(new Pokemon("Rocket", "Evil", 1));
            }

            if (userPokemon.stream().anyMatch(p -> p.getLevel() >= 10)) {
                endGameAndSaveHistory();
                return "redirect:/history";
            }

            return "redirect:/";
        } catch (Exception e) {
            logger.error("An error occurred while catching Pokemon.", e);
            model.addAttribute("error", "An error occurred while catching Pokemon. Check logs for details.");
            return "error";
        }
    }

    @PostMapping("/rocket")
    public String useRocket(Model model) {
        try {
            if (!rocketButtonActivated) {
                Random random = new Random();

                // 33% шанс, что кнопка Ракета появится и вызовет эффект
                if (random.nextInt(3) == 0) {
                    rocketButtonActivated = true;
                    rocketButtonCounter++;

                    // Если кнопка «Ракета» активирована, блокируем остальные кнопки
                    model.addAttribute("rocketButtonActivated", true);

                    // Если кнопка «Ракета» нажата 4 раза, обновим информацию о лидерах
                    if (rocketButtonCounter >= 4) {
                        updateLeader();
                        leaderPokemon.clear();
                        leaderPokemon.add(new Pokemon("Rocket", "Evil", 1));
                        endGameAndSaveHistory();
                        return "redirect:/history";
                    }
                } else {
                    handleRocketEffect(model);
                }
            }

            model.addAttribute("userPokemon", userPokemon);
            model.addAttribute("serverPokemon", serverPokemon);
            model.addAttribute("leader", leaderPokemon.isEmpty() ? null : leaderPokemon.get(0));

            return "redirect:/";
        } catch (Exception e) {
            logger.error("An error occurred while using Rocket.", e);
            model.addAttribute("error", "An error occurred while using Rocket. Check logs for details.");
            return "error";
        }
    }

    // Метод для обработки эффекта кнопки Rocket
    private void handleRocketEffect(Model model) {
        // Убираем из списка пользователя случайного покемона
        if (!userPokemon.isEmpty()) {
            Random randomUserPokemon = new Random();
            int randomUserPokemonIndex = randomUserPokemon.nextInt(userPokemon.size());
            userPokemon.remove(randomUserPokemonIndex);
        }

        // Добавляем 1 уровень Pikachu, если в списке пользователя есть покемоны с одинаковыми стихиями
        if (userPokemon.stream().anyMatch(p -> p.getElement().equals("Electric"))) {
            userPokemon.stream()
                    .filter(p -> p.getElement().equals("Electric"))
                    .forEach(p -> p.setLevel(p.getLevel() + 1));
        } else {
            // В противном случае отнимаем 1 уровень у Pikachu
            userPokemon.stream()
                    .filter(p -> p.getName().equals("Pikachu"))
                    .findFirst()
                    .ifPresent(p -> p.setLevel(Math.max(1, p.getLevel() - 1)));

            // Добавляем в список покемонов сервера новых покемонов из резервного списка
            handleNewPokemonFromReserve();
        }
    }

    // Метод для обработки добавления новых покемонов из резервного списка
    private void handleNewPokemonFromReserve() {
        List<Pokemon> reservePokemon = generateReservePokemon();

        // Добавляем случайное количество покемонов из резерва в список серверных покемонов
        Random random = new Random();
        int newPokemonCount = random.nextInt(reservePokemon.size()) + 1;
        for (int i = 0; i < newPokemonCount; i++) {
            int randomReserveIndex = random.nextInt(reservePokemon.size());
            serverPokemon.add(reservePokemon.get(randomReserveIndex));
            reservePokemon.remove(randomReserveIndex);
        }
    }

    // Генерация списка резервных покемонов
    private List<Pokemon> generateReservePokemon() {
        List<Pokemon> reservePokemon = new ArrayList<>(List.of(
                new Pokemon("Дроузи", "Психический", 1),
                new Pokemon("Старью", "Водный", 1),
                new Pokemon("Понита", "Огненный", 1),
                new Pokemon("Нидоран", "Ядовитый", 1),
                new Pokemon("Эканс", "Ядовитый", 1),
                new Pokemon("Мяут", "Нормальный", 1),
                new Pokemon("Видл", "Насекомое", 1),
                new Pokemon("Джинкс", "Ледяной", 1),
                new Pokemon("Дратини", "Драконий", 1)
                // резервный список
        ));

        // Можно добавить дополнительные покемоны в резерв по необходимости

        return reservePokemon;
    }

    // Метод для обработки трансформации покемона
    @PostMapping("/transformPokemon")
    public String transformPokemon(Model model) {
        try {
            // Проверяем, есть ли покемоны для трансформации
            if (userPokemon.stream().anyMatch(p -> p.getLevel() >= 5)) {
                // Выбираем первого покемона с уровнем 5 и выше для трансформации
                Pokemon transformingPokemon = userPokemon.stream()
                        .filter(p -> p.getLevel() >= 5)
                        .findFirst()
                        .orElseThrow();

                // Выбираем следующую стадию эволюции для покемона
                List<Pokemon> evolutionStage = getEvolutionStage(transformingPokemon);
                if (evolutionStage != null) {
                    // Удаление текущего покемона из списка пользователя
                    userPokemon.remove(transformingPokemon);

                    // Добавление покемона следующей стадии в список пользователя
                    userPokemon.addAll(evolutionStage);

                    // Обновление лидера
                    updateLeader();

                    // Добавление новых покемонов из резерва на сервер
                    handleNewPokemonFromReserve();
                }
            }

            model.addAttribute("userPokemon", userPokemon);
            model.addAttribute("serverPokemon", serverPokemon);
            model.addAttribute("leader", leaderPokemon.isEmpty() ? null : leaderPokemon.get(0));

            return "redirect:/";
        } catch (Exception e) {
            logger.error("An error occurred while transforming Pokemon.", e);
            model.addAttribute("error", "An error occurred while transforming Pokemon. Check logs for details.");
            return "error";
        }
    }

    // Метод для выполнения эволюции
    private void evolvePokemon() {
        // Проходим по каждому покемону в списке пользователя
        for (Pokemon userPokemon : userPokemon) {
            // Проверяем, достиг ли покемон 5 уровня и не является ли он Pikachu
            if (userPokemon.getLevel() >= 5 && !userPokemon.getName().equals("Pikachu")) {
                // Ищем соответствующую стадию эволюции для текущего покемона
                List<Pokemon> evolutionStage = getEvolutionStage(userPokemon);

                // Если есть стадия эволюции, выполняем эволюцию
                if (evolutionStage != null) {
                    // Удаление текущего покемона из списка пользователя
                    userPokemon.setLevel(Math.max(1, userPokemon.getLevel() - 5));

                    // Поиск покемона в списке эволюции с совпадающей стихией
                    Optional<Pokemon> evolvedPokemon = evolutionStage.stream()
                            .filter(p -> p.getElement().equals(userPokemon.getElement()))
                            .findFirst();

                    // Если найден эволюционированный покемон, добавляем его в список пользователя
                    evolvedPokemon.ifPresent(evolved -> {
                        userPokemon.setName(evolved.getName());
                        userPokemon.setLevel(userPokemon.getLevel() + evolved.getLevel());
                    });
                }
            }
        }

        // Добавление новых покемонов из резерва на сервер
        handleNewPokemonFromReserve();
    }

    // Метод для получения стадии эволюции для покемона
    private List<Pokemon> getEvolutionStage(Pokemon pokemon) {

        if (pokemon.getName().equals("Катерпи")) {
            return List.of(new Pokemon("Метапод", "Насекомый", 1));
        }
        if (pokemon.getName().equals("Бульбазавр")) {
            return List.of(new Pokemon("Ивизавр", "Травяной", 1));
        }
        if (pokemon.getName().equals("Чармандер")) {
            return List.of(new Pokemon("Чармилеон", "Огненный", 1));
        }
        if (pokemon.getName().equals("Сквиртл")) {
            return List.of(new Pokemon("Вартортл", "Водный", 1));
        }
        if (pokemon.getName().equals("Дроузи")) {
            return List.of(new Pokemon("Хипно", "Психический", 1));
        }
        if (pokemon.getName().equals("Видл")) {
            return List.of(new Pokemon("Какуна", "Насекомый", 1));
        }
        if (pokemon.getName().equals("Понита")) {
            return List.of(new Pokemon("Рапидаш", "Огненый", 1));
        }
        if (pokemon.getName().equals("Мяут")) {
            return List.of(new Pokemon("Персиан", "Нормальный", 1));
        }

        return null;
    }


    // Метод для отображения истории из файла
    @GetMapping("/history")
    public String showHistory(Model model) {
        try {
            HistoricalResult historicalResult = readHistoryFromFile();
            model.addAttribute("historicalResult", historicalResult);
            return "history";
        } catch (Exception e) {
            logger.error("An error occurred while reading history.", e);
            model.addAttribute("error", "An error occurred while reading history. Check logs for details.");
            return "error";
        }
    }

    // Дополнительный метод для сохранения истории по запросу
    @PostMapping("/saveHistory")
    @ResponseBody
    public ResponseEntity<String> saveHistory(Model model) {
        try {
            endGameAndSaveHistory();
            return ResponseEntity.ok("History saved successfully");
        } catch (Exception e) {
            logger.error("An error occurred while saving history.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error saving history");
        }
    }

    // Метод для обновления лидера
    private void updateLeader() {
        if (leaderPokemon.size() > 1) {
            Pokemon currentLeader = leaderPokemon.get(0);
            Pokemon previousLeader = leaderPokemon.get(1);

            boolean isPikachuAlreadyAffected = currentLeader.getName().equals(previousLeader.getName());

            if (!isPikachuAlreadyAffected) {
                if (userPokemon.stream().anyMatch(p -> p.getName().equals("Pikachu"))) {
                    userPokemon.stream()
                            .filter(p -> p.getName().equals("Pikachu"))
                            .findFirst()
                            .ifPresent(p -> p.setLevel(Math.max(1, p.getLevel() - 2)));
                }
            }
        }
    }

    // Метод для сохранения истории в файл
    HistoricalResult endGameAndSaveHistory() {
        try {
            List<HistoricalPokemon> historicalPokemonList = userPokemon.stream()
                    .map(pokemon -> new HistoricalPokemon(pokemon.getName(), pokemon.getElement(), pokemon.getLevel()))
                    .collect(Collectors.toList());

            int totalLevels = userPokemon.stream().mapToInt(Pokemon::getLevel).sum();

            HistoricalResult historicalResult = new HistoricalResult(historicalPokemonList, totalLevels);

            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));

            File directory = new File("history");

            if (!directory.exists() && !directory.mkdirs()) {
                throw new IOException("Failed to create history directory");
            }

            File file = new File(directory, "history.json");

            try (FileOutputStream fos = new FileOutputStream(file)) {
                mapper.writeValue(fos, historicalResult);
            } catch (IOException e) {
                logger.error("Error writing historical result to file.", e);
                throw e;
            }
        } catch (IOException e) {
            logger.error("An unexpected error occurred.", e);
        }
        return null;
    }

    // Метод для чтения истории из файла
    private HistoricalResult readHistoryFromFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File("history/history.json");

        if (!file.exists()) {
            return new HistoricalResult(new ArrayList<>(), 0);
        }

        return mapper.readValue(file, HistoricalResult.class);
    }
}









