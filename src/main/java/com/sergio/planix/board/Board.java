package com.sergio.planix.board;

import com.sergio.planix.auth.User;
import com.sergio.planix.common.BaseEntity;
import com.sergio.planix.team.Team;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "boards")
@Getter
@Setter
public class Board extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id")
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoardVisibility visibility;

    protected Board() {}

    public Board(Team team, User owner, String name, String description, String icon,
                 BoardVisibility visibility) {
        this.team = team;
        this.owner = owner;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.visibility = visibility;
    }

    public boolean isOpenToTeam() {
        return visibility == BoardVisibility.TEAM;
    }
}
