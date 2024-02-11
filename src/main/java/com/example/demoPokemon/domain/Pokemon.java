package com.example.demoPokemon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pokemon {

    private String name;
    private String element;
    private int level;

    private boolean isLeader;

    public Pokemon(String name, String element, int level) {
        this.name = name;
        this.element = element;
        this.level = level;
        this.isLeader = false;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public void setLeader(boolean leader) {
        isLeader = leader;
    }
}

