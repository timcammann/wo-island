package com.tcammann.woisland.repository;

import com.tcammann.woisland.model.Ranking;
import com.tcammann.woisland.model.ReactionEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;

public interface ReactionEventRepository extends JpaRepository<ReactionEventEntity, Long> {

    @Query("select new com.tcammann.woisland.model.Ranking(t.messageAuthor, count(t.messageAuthor)) " +
            "from ReactionEventEntity t " +
            "where t.server = :server " +
            "and t.timestamp > :after " +
            "group by t.messageAuthor " +
            "order by count(t.messageAuthor) desc")
    Page<Ranking> findTopXByServer(Long server, Date after, Pageable pageable);

}
