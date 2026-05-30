package og.portal.zarplata.service;

import lombok.RequiredArgsConstructor;
import og.portal.zarplata.dto.SalaryColumnMappingDTO;
import og.portal.zarplata.enums.TextStyle;
import og.portal.zarplata.model.SalaryColumnMapping;
import og.portal.zarplata.repository.SalaryColumnMappingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryMappingService {

    private final SalaryColumnMappingRepository repository;

    public List<SalaryColumnMappingDTO> getVisibleColumns() {
        return repository.findByVisibleTrueOrderByExcelColIndexAsc().stream()
                .map(salaryColumnMapping -> new SalaryColumnMappingDTO(
                        salaryColumnMapping.getExcelColIndex(),
                        salaryColumnMapping.getColumnName(),
                        salaryColumnMapping.getAlignment(),
                        getAlignmentClass(salaryColumnMapping)
                ))
                .collect(Collectors.toList());
    }

    private String getAlignmentClass(SalaryColumnMapping entity) {
        if (entity.getAlignment() == null) {
            return TextStyle.START.getCssClass();
        }
        return switch (entity.getAlignment()) {
            case CENTER -> TextStyle.CENTER.getCssClass();
            case RIGHT -> TextStyle.END.getCssClass();
            default -> TextStyle.START.getCssClass();
        };
    }
}