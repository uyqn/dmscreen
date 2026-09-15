package no.uyqn.dmscreen.dice.internal;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import no.uyqn.dmscreen.dice.Advantage;
import no.uyqn.dmscreen.dice.DiceExpression;

@Entity
@Table(schema = "dice", name = "roll")
public class Roll {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Instant rolledAt;
    private String expression;
    private int[] dice;

    @Enumerated(EnumType.STRING)
    private Advantage advantage;

    private int total;

    @Enumerated(EnumType.STRING)
    private Source source;

    private String reason;

    protected Roll() {}

    public Roll(DiceExpression expression, int[] dice, Advantage advantage, int total, String reason) {
        this.rolledAt = Instant.now();
        this.expression = expression.toString();
        this.dice = dice;
        this.advantage = advantage;
        this.total = total;
        this.source = Source.SERVER;
        this.reason = reason;
    }

    public UUID getId() {
        return id;
    }

    public Instant getRolledAt() {
        return rolledAt;
    }

    public String getExpression() {
        return expression;
    }

    public int[] getDice() {
        return dice;
    }

    public Advantage getAdvantage() {
        return advantage;
    }

    public int getTotal() {
        return total;
    }

    public Source getSource() {
        return source;
    }

    public String getReason() {
        return reason;
    }

    public enum Source {
        SERVER,
        PHYSICAL
    }
}
