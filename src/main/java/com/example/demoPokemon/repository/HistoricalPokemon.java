package com.example.demoPokemon.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalPokemon {

    private String name;
    private String element;
    private int level;
}
