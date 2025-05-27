package com.ns.resultquery.adapter.out.persistence;

import com.ns.resultquery.domain.Champ;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChampRepository extends JpaRepository<Champ, Long> {
    List<Champ> findAll();
}
