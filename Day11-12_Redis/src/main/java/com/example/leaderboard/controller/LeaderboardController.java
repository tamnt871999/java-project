package com.example.leaderboard.controller;

import com.example.leaderboard.service.LeaderboardService;
import com.example.leaderboard.service.LeaderboardService.PlayerScore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboard;

    public LeaderboardController(LeaderboardService leaderboard) {
        this.leaderboard = leaderboard;
    }

    @PostMapping("/{username}")
    public PlayerScore submitScore(@PathVariable("username") String username,
                                   @Valid @RequestBody ScoreRequest request) {
        return leaderboard.submitScore(username, request.score());
    }

    @GetMapping("/top/{n}")
    public List<PlayerScore> top(@PathVariable("n") int n) {
        return leaderboard.topN(n);
    }

    public record ScoreRequest(@NotNull Double score) {
    }
}
