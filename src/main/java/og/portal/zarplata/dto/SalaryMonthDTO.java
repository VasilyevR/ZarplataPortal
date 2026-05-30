package og.portal.zarplata.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

public record SalaryMonthDTO(
        String monthName,
        Integer year,
        AtomicReference<BigDecimal> totalAmount,
        List<SalaryRowDTO> rows
) {
    public SalaryMonthDTO(
            String monthName,
            Integer year
    ) {
        this(monthName, year, new AtomicReference<>(BigDecimal.ZERO), new ArrayList<>());
    }

    public AtomicReference<BigDecimal> totalAmount() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public BigDecimal getTotalAmount() {
        return totalAmount.get();
    }

    public void incrementTotalAmount(BigDecimal amount) {
        totalAmount.updateAndGet(v -> v.add(amount));
    }

    public List<SalaryRowDTO> rows() {
        return Collections.unmodifiableList(rows);
    }

    public void addRow(SalaryRowDTO row) {
        rows.add(row);
    }
}