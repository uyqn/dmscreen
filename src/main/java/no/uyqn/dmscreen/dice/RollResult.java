package no.uyqn.dmscreen.dice;

import java.util.List;
import java.util.UUID;

public record RollResult(UUID rollId, DiceExpression expression, List<Integer> dice, Advantage advantage, int total) {}
