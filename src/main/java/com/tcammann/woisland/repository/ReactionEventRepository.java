package com.tcammann.woisland.repository;

import com.tcammann.woisland.model.Ranking;
import com.tcammann.woisland.model.ReactionEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReactionEventRepository extends JpaRepository<ReactionEventEntity, Long> {

    @Query("select new com.tcammann.woisland.model.Ranking(t.toUser, count(t.toUser)) " +
            "from ReactionEventEntity t where " +
            "t.server = :server " +
            "group by t.toUser " +
            "order by count(t.toUser)")
    Page<Ranking> findTopXByServer(Long server, Pageable pageable);

}
