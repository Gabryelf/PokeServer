package com.example.demoPokemon.controller;

import com.example.demoPokemon.repository.HistoricalResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GameController {

    @Autowired
    private PokemonController gameService;

    @PostMapping("/finishGame")
    public ResponseEntity<Map<String, String>> finishGame() {
        try {

            HistoricalResult historicalResult = gameService.endGameAndSaveHistory();


            Map<String, String> response = new HashMap<>();
            response.put("redirectUrl", "/history.html");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status( HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

