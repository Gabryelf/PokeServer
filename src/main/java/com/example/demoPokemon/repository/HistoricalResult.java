package com.example.demoPokemon.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalResult {
    private List<HistoricalPokemon> historicalPokemonList;
    private int totalLevels;
}
