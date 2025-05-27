package com.ns.resultquery.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "champion_stats")
@Entity
public class Champ {
    @Id
    @Column(name = "id")
    private Integer championId;

    private String name;
}
